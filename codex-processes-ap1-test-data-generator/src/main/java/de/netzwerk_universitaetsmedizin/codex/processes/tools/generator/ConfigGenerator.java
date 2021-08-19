package de.netzwerk_universitaetsmedizin.codex.processes.tools.generator;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.netzwerk_universitaetsmedizin.codex.processes.tools.generator.CertificateGenerator.CertificateFiles;

public class ConfigGenerator
{
	private static final Logger logger = LoggerFactory.getLogger(ConfigGenerator.class);

	private static final String P_KEY_SERVER_USER_THUMBPRINTS = "org.highmed.dsf.fhir.server.user.thumbprints";
	private static final String P_KEY_SERVER_USER_THUMBPRINTS_PERMANENTDELETE = "org.highmed.dsf.fhir.server.user.thumbprints.permanent.delete";

	private Properties dockerDicFhirConfigProperties;
	private Properties dockerCrrFhirConfigProperties;
	private Properties dockerGthFhirConfigProperties;

	private Properties readProperties(Path propertiesFile)
	{
		@SuppressWarnings("serial")
		Properties properties = new Properties()
		{
			// making sure entries are sorted when storing properties
			@Override
			public Set<Map.Entry<Object, Object>> entrySet()
			{
				return Collections.synchronizedSet(
						super.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().toString()))
								.collect(Collectors.toCollection(LinkedHashSet::new)));
			}
		};
		try (InputStream in = Files.newInputStream(propertiesFile);
				InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
		{
			properties.load(reader);
		}
		catch (IOException e)
		{
			logger.error("Error while reading properties from " + propertiesFile.toString(), e);
			throw new RuntimeException(e);
		}
		return properties;
	}

	private void writeProperties(Path propertiesFiles, Properties properties)
	{
		try (OutputStream out = Files.newOutputStream(propertiesFiles);
				OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8))
		{
			properties.store(writer, "Generated by test-data-generator");
		}
		catch (IOException e)
		{
			logger.error("Error while writing properties to " + propertiesFiles.toString(), e);
			throw new RuntimeException(e);
		}
	}

	public void modifyDockerTestFhirConfigProperties(Map<String, CertificateFiles> clientCertificateFilesByCommonName)
	{
		modifyDockerDicFhirConfigProperties(clientCertificateFilesByCommonName);
		modifyDockerCrrFhirConfigProperties(clientCertificateFilesByCommonName);
		modifyDockerGthFhirConfigProperties(clientCertificateFilesByCommonName);
	}

	private void modifyDockerDicFhirConfigProperties(Map<String, CertificateFiles> clientCertificateFilesByCommonName)
	{
		CertificateFiles dicClient = clientCertificateFilesByCommonName.get("dic-client");
		CertificateFiles webbrowserTestUser = clientCertificateFilesByCommonName.get("Webbrowser Test User");

		Path dockerTestFhirConfigTemplateFile = Paths
				.get("src/main/resources/config-templates/docker-test-dic-fhir-config.properties");
		dockerDicFhirConfigProperties = readProperties(dockerTestFhirConfigTemplateFile);

		dockerDicFhirConfigProperties.setProperty(P_KEY_SERVER_USER_THUMBPRINTS,
				dicClient.getCertificateSha512ThumbprintHex() + ","
						+ webbrowserTestUser.getCertificateSha512ThumbprintHex());

		dockerDicFhirConfigProperties.setProperty(P_KEY_SERVER_USER_THUMBPRINTS_PERMANENTDELETE,
				dicClient.getCertificateSha512ThumbprintHex() + ","
						+ webbrowserTestUser.getCertificateSha512ThumbprintHex());

		writeProperties(Paths.get("config/docker-test-dic-fhir-config.properties"), dockerDicFhirConfigProperties);
	}

	private void modifyDockerCrrFhirConfigProperties(Map<String, CertificateFiles> clientCertificateFilesByCommonName)
	{
		CertificateFiles crrClient = clientCertificateFilesByCommonName.get("crr-client");
		CertificateFiles webbrowserTestUser = clientCertificateFilesByCommonName.get("Webbrowser Test User");

		Path dockerTestFhirConfigTemplateFile = Paths
				.get("src/main/resources/config-templates/docker-test-crr-fhir-config.properties");
		dockerCrrFhirConfigProperties = readProperties(dockerTestFhirConfigTemplateFile);

		dockerCrrFhirConfigProperties.setProperty(P_KEY_SERVER_USER_THUMBPRINTS,
				crrClient.getCertificateSha512ThumbprintHex() + ","
						+ webbrowserTestUser.getCertificateSha512ThumbprintHex());

		dockerCrrFhirConfigProperties.setProperty(P_KEY_SERVER_USER_THUMBPRINTS_PERMANENTDELETE,
				crrClient.getCertificateSha512ThumbprintHex() + ","
						+ webbrowserTestUser.getCertificateSha512ThumbprintHex());

		writeProperties(Paths.get("config/docker-test-crr-fhir-config.properties"), dockerCrrFhirConfigProperties);
	}

	private void modifyDockerGthFhirConfigProperties(Map<String, CertificateFiles> clientCertificateFilesByCommonName)
	{
		CertificateFiles gthClient = clientCertificateFilesByCommonName.get("gth-client");
		CertificateFiles webbrowserTestUser = clientCertificateFilesByCommonName.get("Webbrowser Test User");

		Path dockerTestFhirConfigTemplateFile = Paths
				.get("src/main/resources/config-templates/docker-test-gth-fhir-config.properties");
		dockerGthFhirConfigProperties = readProperties(dockerTestFhirConfigTemplateFile);

		dockerGthFhirConfigProperties.setProperty(P_KEY_SERVER_USER_THUMBPRINTS,
				gthClient.getCertificateSha512ThumbprintHex() + ","
						+ webbrowserTestUser.getCertificateSha512ThumbprintHex());

		dockerGthFhirConfigProperties.setProperty(P_KEY_SERVER_USER_THUMBPRINTS_PERMANENTDELETE,
				gthClient.getCertificateSha512ThumbprintHex() + ","
						+ webbrowserTestUser.getCertificateSha512ThumbprintHex());

		writeProperties(Paths.get("config/docker-test-gth-fhir-config.properties"), dockerGthFhirConfigProperties);
	}

	public void copyDockerTestFhirConfigProperties()
	{
		Path dockerDicFhirConfigPropertiesFile = Paths.get("config/docker-test-dic-fhir-config.properties");
		logger.info("Copying config.properties to {}", dockerDicFhirConfigPropertiesFile);
		writeProperties(dockerDicFhirConfigPropertiesFile, dockerDicFhirConfigProperties);

		Path dockerCrrFhirConfigPropertiesFile = Paths.get("config/docker-test-crr-fhir-config.properties");
		logger.info("Copying config.properties to {}", dockerCrrFhirConfigPropertiesFile);
		writeProperties(dockerCrrFhirConfigPropertiesFile, dockerCrrFhirConfigProperties);

		Path dockerGthFhirConfigPropertiesFile = Paths.get("config/docker-test-gth-fhir-config.properties");
		logger.info("Copying config.properties to {}", dockerCrrFhirConfigPropertiesFile);
		writeProperties(dockerCrrFhirConfigPropertiesFile, dockerGthFhirConfigProperties);

	}
}
