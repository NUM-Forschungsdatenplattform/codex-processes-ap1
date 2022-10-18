package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_NO_DIC_PSEUDONYM_FOR_BLOOMFILTER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_PATIENT_NOT_FOUND;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.IDENTIFIER_NUM_CODEX_DIC_PSEUDONYM_TYPE_CODE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.IDENTIFIER_NUM_CODEX_DIC_PSEUDONYM_TYPE_SYSTEM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_BLOOM_FILTER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM;

import java.util.Objects;
import java.util.Optional;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.FttpClientFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.GeccoClientFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReference;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReferenceValues;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReferenceValues.PatientReferenceValue;

public class ResolvePsn extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(ResolvePsn.class);

	private final GeccoClientFactory geccoClientFactory;
	private final FttpClientFactory fttpClientFactory;

	public ResolvePsn(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, GeccoClientFactory geccoClientFactory,
			FttpClientFactory fttpClientFactory)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.geccoClientFactory = geccoClientFactory;
		this.fttpClientFactory = fttpClientFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(geccoClientFactory, "geccoClientFactory");
		Objects.requireNonNull(fttpClientFactory, "fttpClientFactory");
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws Exception
	{
		String reference = ((PatientReference) execution.getVariable(BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE))
				.getAbsoluteReference();

		logger.info("Resolving DIC pseudonym for patient {}", reference);

		getPatient(reference).ifPresentOrElse(patient ->
		{
			getPseudonym(patient).ifPresentOrElse(pseudonym ->
			{
				logger.debug("Patient {} has DIC pseudonym {}", reference, pseudonym);
				execution.setVariable(BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE, getPatientReference(pseudonym));
			}, () ->
			{
				logger.debug("Patient {} has no DIC pseudonym", reference);
				String pseudonym = resolvePseudonymAndUpdatePatient(patient);
				execution.setVariable(BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE, getPatientReference(pseudonym));
			});
		}, () ->
		{
			logger.warn("Patient {} not found", reference);
			throw new BpmnError(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_PATIENT_NOT_FOUND,
					"Patient at " + reference + " not found");
		});
	}

	private Optional<Patient> getPatient(String reference)
	{
		return geccoClientFactory.getGeccoClient().getFhirClient().getPatient(reference);
	}

	private Optional<String> getPseudonym(Patient patient)
	{
		return patient.getIdentifier().stream().filter(i -> i.getSystem().equals(NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM))
				.findFirst().map(Identifier::getValue);
	}

	private String resolvePseudonymAndUpdatePatient(Patient patient)
	{
		String bloomFilter = getBloomFilter(patient);
		String pseudonym = resolveBloomFilter(bloomFilter);

		patient.getIdentifier().removeIf(i -> NAMING_SYSTEM_NUM_CODEX_BLOOM_FILTER.equals(i.getSystem()));
		patient.addIdentifier().setSystem(NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM).setValue(pseudonym).getType()
				.getCodingFirstRep().setSystem(IDENTIFIER_NUM_CODEX_DIC_PSEUDONYM_TYPE_SYSTEM)
				.setCode(IDENTIFIER_NUM_CODEX_DIC_PSEUDONYM_TYPE_CODE);

		updatePatient(patient);

		return pseudonym;
	}

	private String getBloomFilter(Patient patient)
	{
		return patient.getIdentifier().stream().filter(Identifier::hasSystem)
				.filter(i -> NAMING_SYSTEM_NUM_CODEX_BLOOM_FILTER.equals(i.getSystem())).filter(Identifier::hasValue)
				.findFirst().map(Identifier::getValue).orElseThrow(() -> new RuntimeException(
						"No bloom filter present in patient " + patient.getIdElement().getValue()));
	}

	private String resolveBloomFilter(String bloomFilter)
	{
		return fttpClientFactory.getFttpClient().getDicPseudonym(bloomFilter)
				.orElseThrow(() -> new BpmnError(
						CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_NO_DIC_PSEUDONYM_FOR_BLOOMFILTER,
						"Unable to get DIC pseudonym for given BloomFilter"));
	}

	private void updatePatient(Patient patient)
	{
		geccoClientFactory.getGeccoClient().getFhirClient().updatePatient(patient);
	}

	private PatientReferenceValue getPatientReference(String pseudonym)
	{
		return PatientReferenceValues.create(PatientReference
				.from(new Identifier().setSystem(NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM).setValue(pseudonym)));
	}
}