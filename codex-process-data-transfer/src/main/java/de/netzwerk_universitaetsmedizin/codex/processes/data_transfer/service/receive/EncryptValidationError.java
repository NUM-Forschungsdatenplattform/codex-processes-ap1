package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_AES_RETURN_KEY;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BUNDLE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PSEUDONYM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_ECRYPTION_OF_VALIDATION_ERROR_FOR_DIC_FAILED;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_CRR_PSEUDONYM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PSEUDONYM_PLACEHOLDER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.RETURN_AAD;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.pseudonymization.crypto.AesGcmUtil;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;

public class EncryptValidationError extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(EncryptValidationError.class);

	private final FhirContext fhirContext;

	public EncryptValidationError(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, FhirContext fhirContext)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.fhirContext = fhirContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(fhirContext, "fhirContext");
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws BpmnError, Exception
	{
		Bundle bundle = (Bundle) execution.getVariable(BPMN_EXECUTION_VARIABLE_BUNDLE);
		String pseudonym = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_PSEUDONYM);
		byte[] returnKey = (byte[]) execution.getVariable(BPMN_EXECUTION_VARIABLE_AES_RETURN_KEY);

		try
		{
			byte[] bundleData = toByteArray(pseudonym, bundle);
			byte[] encrypted = AesGcmUtil.encrypt(bundleData, RETURN_AAD, new SecretKeySpec(returnKey, "AES"));
			execution.setVariable(BPMN_EXECUTION_VARIABLE_BUNDLE, Variables.byteArrayValue(encrypted));
		}
		catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException
				| ShortBufferException e)
		{
			logger.warn("Unable to encrypt validation error for DIC: " + e.getMessage(), e);
			throw new BpmnError(
					CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_ECRYPTION_OF_VALIDATION_ERROR_FOR_DIC_FAILED,
					"Unable to encrypt validation error for DIC");
		}
	}

	private byte[] toByteArray(String pseudonym, Bundle bundle) throws IOException
	{
		String bundleString = fhirContext.newJsonParser().encodeResourceToString(bundle);

		return bundleString.replace(pseudonym, PSEUDONYM_PLACEHOLDER)
				.replace(NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM, NAMING_SYSTEM_NUM_CODEX_CRR_PSEUDONYM)
				.getBytes(StandardCharsets.UTF_8);
	}
}