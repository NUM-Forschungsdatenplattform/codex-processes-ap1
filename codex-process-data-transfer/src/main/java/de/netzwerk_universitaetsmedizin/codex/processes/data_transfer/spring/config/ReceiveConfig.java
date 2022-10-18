package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message.ContinueTranslateProcess;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message.ContinueTranslateProcessWithError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message.ContinueTranslateProcessWithValidationError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive.DecryptData;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive.DeleteValidationErrorForGth;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive.DownloadDataFromGth;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive.EncryptValidationError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive.InsertDataIntoCodex;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive.LogError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive.LogSuccess;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive.LogValidationError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive.StoreValidationErrorForGth;

@Configuration
public class ReceiveConfig
{
	@Autowired
	private TransferDataConfig transferDataConfig;

	@Bean
	public DownloadDataFromGth downloadDataFromGth()
	{
		return new DownloadDataFromGth(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper(), transferDataConfig.endpointProvider(),
				transferDataConfig.gthIdentifierValue());
	}

	@Bean
	public DecryptData decryptData()
	{
		return new DecryptData(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper(), transferDataConfig.fhirContext(),
				transferDataConfig.crrKeyProvider());
	}

	@Bean
	public InsertDataIntoCodex insertDataIntoCodex()
	{
		return new InsertDataIntoCodex(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper(), transferDataConfig.geccoClientFactory(),
				transferDataConfig.dataLogger());
	}

	@Bean
	public ContinueTranslateProcess continueTranslateProcess()
	{
		return new ContinueTranslateProcess(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper(), transferDataConfig.organizationProvider(),
				transferDataConfig.fhirContext());
	}

	@Bean(name = "Receive-logSuccess") // prefix to force distinct bean names
	public LogSuccess logSuccess()
	{
		return new LogSuccess(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper());
	}

	@Bean(name = "Receive-logValidation") // prefix to force distinct bean names
	public LogValidationError logValidationError()
	{
		return new LogValidationError(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper(), transferDataConfig.errorOutputParameterGenerator(),
				transferDataConfig.errorLogger());
	}

	@Bean
	public EncryptValidationError encryptValidationError()
	{
		return new EncryptValidationError(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper(), transferDataConfig.fhirContext());
	}

	@Bean
	public StoreValidationErrorForGth storeValidationErrorForGth()
	{
		return new StoreValidationErrorForGth(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper(), transferDataConfig.endpointProvider(),
				transferDataConfig.gthIdentifierValue());
	}

	@Bean
	public ContinueTranslateProcessWithValidationError continueTranslateProcessWithValidationError()
	{
		return new ContinueTranslateProcessWithValidationError(transferDataConfig.fhirClientProvider(),
				transferDataConfig.taskHelper(), transferDataConfig.readAccessHelper(),
				transferDataConfig.organizationProvider(), transferDataConfig.fhirContext());
	}

	@Bean
	public DeleteValidationErrorForGth deleteValidationErrorForGth()
	{
		return new DeleteValidationErrorForGth(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper());
	}

	@Bean
	public ContinueTranslateProcessWithError continueTranslateProcessWithError()
	{
		return new ContinueTranslateProcessWithError(transferDataConfig.fhirClientProvider(),
				transferDataConfig.taskHelper(), transferDataConfig.readAccessHelper(),
				transferDataConfig.organizationProvider(), transferDataConfig.fhirContext(),
				transferDataConfig.errorInputParameterGenerator());
	}

	@Bean(name = "Receive-logError") // prefix to force distinct bean names
	public LogError logError()
	{
		return new LogError(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper(), transferDataConfig.errorOutputParameterGenerator(),
				transferDataConfig.errorLogger());
	}
}