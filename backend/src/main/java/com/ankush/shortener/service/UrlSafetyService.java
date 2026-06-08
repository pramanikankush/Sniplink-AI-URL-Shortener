package com.ankush.shortener.service;

import com.ankush.shortener.config.AppProperties;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.model.scoring.onnx.OnnxScoringModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Returns a 0..1 risk score for a URL.
 *
 * Implementation strategy:
 * - If a local ONNX model is configured, we load it via LangChain4j's
 *   {@link OnnxScoringModel} and run inference entirely offline. The model
 *   is a BERT cross-encoder that scores a (query, text) pair.
 * - Otherwise we fall back to a deterministic heuristic scorer that
 *   mimics the output range. This keeps the app bootable without a
 *   model file bundled into the repo (ONNX weights are binary and large).
 *
 * Both paths satisfy the contract: no network calls, no paid APIs.
 */
@Service
public class UrlSafetyService {

    private static final Logger log = LoggerFactory.getLogger(UrlSafetyService.class);

    /** Cross-encoder query used to elicit a 0..1 "is this risky?" score. */
    private static final String RISK_QUERY = "Is this URL risky or likely used for phishing, scams, or malware?";

    private static final Pattern SUSPICIOUS_TLD =
            Pattern.compile("\\.(xyz|top|buzz|click|gq|ml|tk|country|stream|download|zip|mov)(/|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern IP_HOST =
            Pattern.compile("^https?://\\d{1,3}(\\.\\d{1,3}){3}");
    private static final Pattern LONG_PATH =
            Pattern.compile(".{120,}");
    private static final Pattern BRAND_TYPOSQUAT =
            Pattern.compile("(paypa1|g00gle|arnazon|micros0ft|app1e|netfl1x)", Pattern.CASE_INSENSITIVE);
    private static final Pattern URL_SHORTENER_CHAIN =
            Pattern.compile("^https?://(bit\\.ly|tinyurl\\.com|t\\.co|goo\\.gl|is\\.gd|ow\\.ly|buff\\.ly)/", Pattern.CASE_INSENSITIVE);

    private final AppProperties props;
    private final ScoringModel model; // may be null if not configured

    public UrlSafetyService(AppProperties props) {
        this.props = props;
        this.model = loadModel(props);
    }

    private ScoringModel loadModel(AppProperties props) {
        String modelPath = props.safety().modelPath();
        String vocabPath = props.safety().vocabPath();
        if (modelPath == null || modelPath.isBlank() || vocabPath == null || vocabPath.isBlank()) {
            log.info("No ONNX model configured — using heuristic risk scorer");
            return null;
        }
        try {
            return new OnnxScoringModel(modelPath, vocabPath);
        } catch (Exception e) {
            log.warn("Failed to load ONNX model at {} — falling back to heuristic: {}",
                    modelPath, e.getMessage());
            return null;
        }
    }

    public double score(String url) {
        if (!props.safety().enabled()) return 0.0;
        double heuristic = heuristicScore(url);

        if (model != null) {
            try {
                Response<Double> r = model.score(TextSegment.from(url), RISK_QUERY);
                double modelScore = r.content();
                if (!Double.isFinite(modelScore)) modelScore = 0.0;
                return clamp(Math.max(heuristic, modelScore));
            } catch (Exception e) {
                log.warn("ONNX inference failed, using heuristic only: {}", e.getMessage());
            }
        }
        return clamp(heuristic);
    }

    private static double heuristicScore(String url) {
        double score = 0.0;
        if (url.length() > 200)           score += 0.15;
        if (LONG_PATH.matcher(url).find()) score += 0.10;
        if (IP_HOST.matcher(url).find())   score += 0.25;
        if (SUSPICIOUS_TLD.matcher(url).find()) score += 0.25;
        if (BRAND_TYPOSQUAT.matcher(url).find()) score += 0.35;
        if (URL_SHORTENER_CHAIN.matcher(url).find()) score += 0.10;
        // penalise excessive subdomains (phishing pattern)
        String host = url.replaceFirst("^https?://", "").split("/")[0];
        int dots = (int) host.chars().filter(c -> c == '.').count();
        if (dots >= 4) score += 0.15;
        // penalise credential-style query strings
        if (url.toLowerCase().contains("password=") || url.toLowerCase().contains("login=")) {
            score += 0.20;
        }
        return score;
    }

    private static double clamp(double v) {
        if (v < 0) return 0;
        if (v > 1) return 1;
        // round to 2 decimals for stable output
        return Math.round(v * 100.0) / 100.0;
    }
}
