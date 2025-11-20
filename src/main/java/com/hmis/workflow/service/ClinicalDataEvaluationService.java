package com.hmis.workflow.service;

import com.hmis.workflow.domain.entity.ClinicalDataPoint;
import com.hmis.workflow.domain.entity.DecisionLogic;
import com.hmis.workflow.domain.entity.WorkflowInstance;
import com.hmis.workflow.repository.ClinicalDataPointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for evaluating clinical decision logic against patient vital signs and measurements
 * Enables dynamic workflow branching based on current patient clinical data
 * Example: "If O2 saturation < 90, route to ICU monitoring path"
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClinicalDataEvaluationService {

    private final ClinicalDataPointRepository clinicalDataPointRepository;

    /**
     * Evaluate a decision logic rule against latest patient clinical data
     * @param decisionLogic The decision logic rule to evaluate
     * @param workflowInstance The workflow instance context
     * @return true if condition is met, false otherwise
     */
    public boolean evaluateDecisionLogic(DecisionLogic decisionLogic, WorkflowInstance workflowInstance) {
        log.info("Evaluating decision logic: {} for patient: {}",
                decisionLogic.getName(), workflowInstance.getPatient().getId());

        try {
            // Get the latest clinical data point for the data point specified in decision logic
            Optional<ClinicalDataPoint> dataPointOpt = clinicalDataPointRepository
                    .findLatestByPatientAndCode(
                            workflowInstance.getPatient().getId(),
                            decisionLogic.getDataPoint());

            if (dataPointOpt.isEmpty()) {
                log.warn("No clinical data found for code: {} patient: {}",
                        decisionLogic.getDataPoint(), workflowInstance.getPatient().getId());
                return false;
            }

            ClinicalDataPoint dataPoint = dataPointOpt.get();

            // Parse expected value as BigDecimal
            BigDecimal expectedValue = new BigDecimal(decisionLogic.getExpectedValue());

            // Evaluate the condition
            boolean result = dataPoint.evaluateCondition(decisionLogic.getOperator(), expectedValue);

            log.info("Decision logic evaluation result: {} (value: {} {} {})",
                    result, dataPoint.getValue(), decisionLogic.getOperator(), decisionLogic.getExpectedValue());

            return result;
        } catch (Exception e) {
            log.error("Error evaluating decision logic: {}", decisionLogic.getId(), e);
            return false;
        }
    }

    /**
     * Get latest clinical data for a specific data point code
     * @param patientId Patient ID
     * @param dataPointCode Data point code (e.g., "O2_SAT", "HR")
     * @return Latest clinical data point or empty if not found
     */
    public Optional<ClinicalDataPoint> getLatestClinicalData(UUID patientId, String dataPointCode) {
        return clinicalDataPointRepository.findLatestByPatientAndCode(patientId, dataPointCode);
    }

    /**
     * Get all clinical data for a patient from a specific time onwards
     * @param patientId Patient ID
     * @param fromTime Starting time
     * @return List of clinical data points
     */
    public List<ClinicalDataPoint> getClinicalDataSince(UUID patientId, LocalDateTime fromTime) {
        return clinicalDataPointRepository.findByPatientAndTimeRange(patientId, fromTime);
    }

    /**
     * Get all recorded values for a specific data point code
     * @param patientId Patient ID
     * @param dataPointCode Data point code
     * @return Historical values
     */
    public List<ClinicalDataPoint> getClinicalDataHistory(UUID patientId, String dataPointCode) {
        return clinicalDataPointRepository.findByPatientAndCode(patientId, dataPointCode);
    }

    /**
     * Check if patient has any abnormal clinical values
     * @param patientId Patient ID
     * @return true if any abnormal values exist
     */
    public boolean hasAbnormalValues(UUID patientId) {
        List<ClinicalDataPoint> abnormal = clinicalDataPointRepository.findAbnormalByPatient(patientId);
        return !abnormal.isEmpty();
    }

    /**
     * Get all abnormal clinical values for a patient
     * @param patientId Patient ID
     * @return List of abnormal values
     */
    public List<ClinicalDataPoint> getAbnormalValues(UUID patientId) {
        return clinicalDataPointRepository.findAbnormalByPatient(patientId);
    }

    /**
     * Check if a patient's clinical data meets a specific threshold
     * @param patientId Patient ID
     * @param dataPointCode Data point code
     * @param operator Comparison operator (>, <, >=, <=, ==, !=)
     * @param threshold Threshold value
     * @return true if condition is met
     */
    public boolean checkClinicalThreshold(UUID patientId, String dataPointCode, String operator, BigDecimal threshold) {
        Optional<ClinicalDataPoint> dataPointOpt = getLatestClinicalData(patientId, dataPointCode);

        if (dataPointOpt.isEmpty()) {
            log.warn("No clinical data found for threshold check: code={}, patient={}", dataPointCode, patientId);
            return false;
        }

        return dataPointOpt.get().evaluateCondition(operator, threshold);
    }

    /**
     * Record a new clinical data point
     * @param patientId Patient ID
     * @param dataPointCode Code for this data point type
     * @param displayName Display name
     * @param value The measured value
     * @param unit Unit of measurement
     * @param measuredByUser User who recorded this
     * @return The created clinical data point
     */
    public ClinicalDataPoint recordClinicalData(
            UUID patientId,
            String dataPointCode,
            String displayName,
            String value,
            String unit,
            String measuredByUser) {

        log.info("Recording clinical data: code={} value={} patient={}", dataPointCode, value, patientId);

        // Note: Patient loading would need to be done through PatientRepository
        // This is a simplified version - in real implementation, fetch patient from DB

        ClinicalDataPoint dataPoint = ClinicalDataPoint.builder()
                .dataPointCode(dataPointCode)
                .displayName(displayName)
                .value(value)
                .unit(unit)
                .numericValue(parseNumericValue(value))
                .measurementTime(LocalDateTime.now())
                .measuredByUser(measuredByUser)
                .isNormal(false) // Would be computed based on reference ranges
                .build();

        // Note: Patient would need to be set from repository in actual implementation
        return dataPoint;
    }

    /**
     * Parse numeric value from string representation
     */
    private BigDecimal parseNumericValue(String value) {
        try {
            // Handle values like "120/80" for blood pressure - return first value
            if (value.contains("/")) {
                return new BigDecimal(value.split("/")[0]);
            }
            // Remove non-numeric characters except decimal point
            String cleaned = value.replaceAll("[^0-9.-]", "");
            return new BigDecimal(cleaned);
        } catch (Exception e) {
            log.warn("Could not parse numeric value: {}", value);
            return null;
        }
    }

    /**
     * Summary of clinical data for a patient
     */
    public ClinicalDataSummary getClinicalDataSummary(UUID patientId) {
        List<ClinicalDataPoint> recentData = clinicalDataPointRepository.findRecentByPatient(patientId);
        List<ClinicalDataPoint> abnormalData = clinicalDataPointRepository.findAbnormalByPatient(patientId);
        List<String> dataPointCodes = clinicalDataPointRepository.findDistinctDataPointCodesByPatient(patientId);

        return ClinicalDataSummary.builder()
                .patientId(patientId)
                .totalDataPoints(recentData.size())
                .abnormalCount(abnormalData.size())
                .dataPointTypes(dataPointCodes)
                .hasAbnormalValues(abnormalData.size() > 0)
                .lastUpdateTime(recentData.isEmpty() ? null : recentData.get(0).getMeasurementTime())
                .build();
    }

    /**
     * Summary of clinical data for a patient
     */
    @lombok.Data
    @lombok.Builder
    public static class ClinicalDataSummary {
        private UUID patientId;
        private int totalDataPoints;
        private int abnormalCount;
        private List<String> dataPointTypes;
        private boolean hasAbnormalValues;
        private LocalDateTime lastUpdateTime;
    }
}
