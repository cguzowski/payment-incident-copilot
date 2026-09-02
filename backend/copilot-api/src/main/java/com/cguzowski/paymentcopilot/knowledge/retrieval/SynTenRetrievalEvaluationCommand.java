package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.nio.file.Path;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.knowledge.retrieval-evaluation", name = "enabled", havingValue = "true")
class SynTenRetrievalEvaluationCommand implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SynTenRetrievalEvaluationCommand.class);

    private final SynTenRetrievalEvaluationSeedRepository seedRepository;
    private final SynTenRetrievalEvaluationContractRepository contractRepository;
    private final SynTenRetrievalEvaluationService evaluationService;
    private final SynTenRetrievalEvaluationArtifactWriter artifactWriter;

    SynTenRetrievalEvaluationCommand(
            SynTenRetrievalEvaluationSeedRepository seedRepository,
            SynTenRetrievalEvaluationContractRepository contractRepository,
            SynTenRetrievalEvaluationService evaluationService,
            SynTenRetrievalEvaluationArtifactWriter artifactWriter) {
        this.seedRepository = seedRepository;
        this.contractRepository = contractRepository;
        this.evaluationService = evaluationService;
        this.artifactWriter = artifactWriter;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        SynTenRetrievalEvaluationSeed seed = seedRepository.load();
        SynTenRetrievalEvaluationContract contract = contractRepository.load(seed.evaluatedAt());
        SynTenRetrievalEvaluationRun result = evaluationService.evaluate(seed, contract);
        Path resultPath = artifactWriter.write(result);
        if (!result.grade().passed()) {
            String failedVariants = result.grade().variants().stream()
                    .filter(grade -> !grade.passed())
                    .map(grade -> grade.caseId() + "/" + grade.variantId())
                    .collect(Collectors.joining(", "));
            throw new SynTenRetrievalEvaluationFailedException(
                    "SynTen retrieval evaluation failed for " + failedVariants + ". Result: " + resultPath);
        }
        LOGGER.info("SynTen retrieval evaluation passed. Result: {}", resultPath);
    }
}

final class SynTenRetrievalEvaluationFailedException extends IllegalStateException {

    SynTenRetrievalEvaluationFailedException(String message) {
        super(message);
    }
}
