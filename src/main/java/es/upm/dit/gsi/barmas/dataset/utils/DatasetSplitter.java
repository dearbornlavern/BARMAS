/*******************************************************************************
 * Copyright (c) 2013 alvarocarrera Grupo de Sistemas Inteligentes - Universidad Politécnica de Madrid. (GSI-UPM)
 * http://www.gsi.dit.upm.es/
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * 
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 * 
 * Contributors:
 *     alvarocarrera - initial API and implementation
 ******************************************************************************/
/**
 * es.upm.dit.gsi.barmas.kowlancz.dataset.utils.DatasetSplitter.java
 */
package es.upm.dit.gsi.barmas.dataset.utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVSaver;
import weka.core.converters.ConverterUtils.DataSource;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

/**
 * Project: barmas File:
 * es.upm.dit.gsi.barmas.dataset.utils.kowlancz.DatasetSplitter.java
 * 
 * Grupo de Sistemas Inteligentes Departamento de Ingeniería de Sistemas
 * Telemáticos Universidad Politécnica de Madrid (UPM)
 * 
 * @author alvarocarrera
 * @email a.carrera@upm.es
 * @twitter @alvarocarrera
 * @date 31/10/2013
 * @version 0.2
 * 
 */
public class DatasetSplitter {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		DatasetSplitter splitter = new DatasetSplitter();

		String originalDatasetPath = "src/main/resources/dataset/kr-vs-k.csv";
		String outputParentDir = "../experiments/output-data-splitter/chess";
		Logger logger = Logger.getLogger(DatasetSplitter.class.getSimpleName());

		// // Experiment 1
		// String outputDir = outputParentDir;
		// splitter.splitDataset(0.3, 4, originalDatasetPath, outputDir, true,
		// "CZ02", logger, 2);
		//
		// // Experiment 2
		// outputDir = outputParentDir;
		// splitter.splitDataset(0.3, 8, originalDatasetPath, outputDir, true,
		// "CZ02", logger, 2);
		//
		// // Experiment 3
		// outputDir = outputParentDir;
		// splitter.splitDataset(3, 2, 4, originalDatasetPath, outputDir,
		// "CZ02", logger);

		// Experiment 3
		String outputDir = outputParentDir;
		splitter.splitDataset(10, 2, 10, originalDatasetPath, outputDir, "chess", logger);

	}

	/**
	 * This method splits the original dataset in many small datasets for a
	 * given number of agents.
	 * 
	 * @param ratio
	 *            0 < ratio < 1 -> Normally, 0.3 or 0.4 to build a test dataset
	 *            with this percentage of the original data.
	 * @param agents
	 *            number of agents to split the original dataset
	 * @param originalDatasetPath
	 * @param outputDir
	 * @param central
	 *            true to create a bayescentral dataset that joint all agent
	 *            data
	 * @param scenario
	 * @param iteration
	 * @throws Exception
	 */
	public void splitDataset(double ratio, int agents, String originalDatasetPath,
			String outputDir, boolean central, String scenario, Logger logger, int iteration) {

		int ratioint = (int) (ratio * 100);
		double roundedratio = ((double) ratioint) / 100;
		String outputDirWithRatio = outputDir + "/" + roundedratio + "testRatio/iteration-"
				+ iteration;
		File dir = new File(outputDirWithRatio);
		if (!dir.exists() || !dir.isDirectory()) {
			dir.mkdirs();
		}

		logger.finer("--> splitDataset()");
		logger.fine("Creating experiment.info...");
		this.createExperimentInfoFile(ratio, agents, originalDatasetPath, outputDirWithRatio,
				central, scenario, logger);

		try {
			// Look for essentials
			List<String[]> essentials = this.getEssentials(originalDatasetPath, logger);

			HashMap<String, CsvWriter> writers = new HashMap<String, CsvWriter>();
			CsvReader csvreader = new CsvReader(new FileReader(new File(originalDatasetPath)));

			csvreader.readHeaders();
			String[] headers = csvreader.getHeaders();
			int originalDatasetRowsCounter = 0;
			while (csvreader.readRecord()) {
				originalDatasetRowsCounter++;
			}
			csvreader.close();

			// Create datasets files

			// Central dataset
			if (central) {
				String fileName = outputDirWithRatio + File.separator + "bayes-central-dataset.csv";
				CsvWriter writer = new CsvWriter(new FileWriter(fileName), ',');
				writer.writeRecord(headers);
				writers.put("CENTRAL", writer);
				for (String[] essential : essentials) {
					writer.writeRecord(essential);
				}
				logger.fine("Bayes central dataset created.");
			}

			// Agent datasets
			String agentsDatasetsDir = outputDirWithRatio + File.separator + agents + "agents";
			File f = new File(agentsDatasetsDir);
			if (!f.isDirectory()) {
				f.mkdirs();
			}
			for (int i = 0; i < agents; i++) {
				String fileName = agentsDatasetsDir + File.separator + "agent-" + i
						+ "-dataset.csv";
				CsvWriter writer = new CsvWriter(new FileWriter(fileName), ',');
				writer.writeRecord(headers);
				for (String[] essential : essentials) {
					writer.writeRecord(essential);
				}
				writers.put("AGENT" + i, writer);
				logger.fine("AGENT" + i + " dataset created.");
			}

			// Test dataset
			String fileName = outputDirWithRatio + File.separator + "test-dataset.csv";
			CsvWriter writer = new CsvWriter(new FileWriter(fileName), ',');
			writer.writeRecord(headers);
			writers.put("TEST", writer);
			logger.fine("Test dataset created.");

			// Create an ordering queue
			int testCases = (int) (ratio * originalDatasetRowsCounter);
			int testStep = originalDatasetRowsCounter / testCases;

			csvreader = new CsvReader(new FileReader(new File(originalDatasetPath)));

			csvreader.readHeaders();
			int stepCounter = 0 - (iteration % testStep);
			int agentCounter = 0;
			while (csvreader.readRecord()) {
				String[] row = csvreader.getValues();
				if (stepCounter % testStep == 0) {
					writer = writers.get("TEST");
					writer.writeRecord(row);
				} else {
					writer = writers.get("AGENT" + agentCounter);
					writer.writeRecord(row);
					writer = writers.get("CENTRAL");
					writer.writeRecord(row);
					agentCounter++;
					if (agentCounter == agents) {
						agentCounter = 0;
					}
				}
				stepCounter++;
			}

			csvreader.close();
			for (CsvWriter w : writers.values()) {
				w.close();
			}

		} catch (Exception e) {
			logger.severe("Exception while splitting dataset. ->");
			logger.severe(e.getMessage());
			System.exit(1);
		}

		logger.finer("<-- splitDataset()");
	}

	/**
	 * This method splits the original dataset in many small datasets for a
	 * given number of agents.
	 * 
	 * This method uses folds generated by WEKA and appends the language at the
	 * end of the datasets (i.e. the essentials).
	 * 
	 * @param folds
	 *            KFold number
	 * @param minAgents
	 *            min number of agents to split the original dataset
	 * @param maxAgents
	 *            max number of agents to split the original dataset
	 * @param originalDatasetPath
	 * @param outputDir
	 * @param central
	 *            true to create a bayescentral dataset that joint all agent
	 *            data
	 * @param scenario
	 */
	/**
	 * @param folds
	 * @param minAgents
	 * @param maxAgents
	 * @param originalDatasetPath
	 * @param outputDir
	 * @param scenario
	 * @param logger
	 */
	/**
	 * @param folds
	 * @param minAgents
	 * @param maxAgents
	 * @param originalDatasetPath
	 * @param outputDir
	 * @param scenario
	 * @param logger
	 */
	public void splitDataset(int folds, int minAgents, int maxAgents, String originalDatasetPath,
			String outputDir, String scenario, Logger logger) {

		int ratioint = (int) ((1 / (double) folds) * 100);
		double roundedratio = ((double) ratioint) / 100;

		// Look for essentials
		List<String[]> essentials = this.getEssentials(originalDatasetPath, logger);

		for (int fold = 0; fold < folds; fold++) {
			String outputDirWithRatio = outputDir + "/" + roundedratio + "testRatio/iteration-"
					+ fold;
			File dir = new File(outputDirWithRatio);
			if (!dir.exists() || !dir.isDirectory()) {
				dir.mkdirs();
			}

			logger.finer("--> splitDataset()");
			logger.fine("Creating experiment.info...");

			try {

				Instances originalData = this.getDataFromCSV(originalDatasetPath);

				originalData.randomize(new Random());
				originalData.stratify(folds);

				// TestDataSet
				Instances testData = originalData.testCV(folds, fold);
				CSVSaver saver = new CSVSaver();
				ArffSaver arffsaver = new ArffSaver();
				File file = new File(outputDirWithRatio + File.separator + "test-dataset.csv");
				if (!file.exists()) {
					saver.resetOptions();
					saver.setInstances(testData);
					saver.setFile(file);
					saver.writeBatch();
				}

				file = new File(outputDirWithRatio + File.separator + "test-dataset.arff");
				if (!file.exists()) {
					arffsaver.resetOptions();
					arffsaver.setInstances(testData);
					arffsaver.setFile(file);
					arffsaver.writeBatch();
				}

				// BayesCentralDataset
				Instances trainData = originalData.trainCV(folds, fold);
				file = new File(outputDirWithRatio + File.separator + "bayes-central-dataset.csv");
				if (!file.exists()) {
					saver.resetOptions();
					saver.setInstances(trainData);
					saver.setFile(file);
					saver.writeBatch();
					this.copyFileUsingApacheCommonsIO(file, new File(outputDirWithRatio
							+ File.separator + "bayes-central-dataset-noEssentials.csv"), logger);
					CsvWriter w = new CsvWriter(new FileWriter(file, true), ',');
					for (String[] essential : essentials) {
						w.writeRecord(essential);
					}
					w.close();
				}
				file = new File(outputDirWithRatio + File.separator + "bayes-central-dataset.arff");
				if (!file.exists()) {
					arffsaver.resetOptions();
					arffsaver.setInstances(trainData);
					arffsaver.setFile(file);
					arffsaver.writeBatch();
					this.copyFileUsingApacheCommonsIO(file, new File(outputDirWithRatio
							+ File.separator + "bayes-central-dataset-noEssentials.arff"), logger);
					CsvWriter w = new CsvWriter(new FileWriter(file, true), ',');
					for (String[] essential : essentials) {
						w.writeRecord(essential);
					}
					w.close();
				}

				// Agent datasets
				CsvReader csvreader = new CsvReader(new FileReader(new File(originalDatasetPath)));
				csvreader.readHeaders();
				String[] headers = csvreader.getHeaders();
				csvreader.close();

				for (int agents = minAgents; agents <= maxAgents; agents++) {
					this.createExperimentInfoFile(folds, agents, originalDatasetPath,
							outputDirWithRatio, scenario, logger);
					HashMap<String, CsvWriter> writers = new HashMap<String, CsvWriter>();
					String agentsDatasetsDir = outputDirWithRatio + File.separator + agents
							+ "agents";
					HashMap<String, CsvWriter> arffWriters = new HashMap<String, CsvWriter>();
					File f = new File(agentsDatasetsDir);
					if (!f.isDirectory()) {
						f.mkdirs();
					}
					Instances copy = new Instances(trainData);
					copy.delete();
					for (int i = 0; i < agents; i++) {
						String fileName = agentsDatasetsDir + File.separator + "agent-" + i
								+ "-dataset.csv";
						file = new File(fileName);
						if (!file.exists()) {
							CsvWriter writer = new CsvWriter(new FileWriter(fileName), ',');
							writer.writeRecord(headers);
							writers.put("AGENT" + i, writer);
						}
						fileName = agentsDatasetsDir + File.separator + "agent-" + i
								+ "-dataset.arff";
						file = new File(fileName);
						if (!file.exists()) {
							arffsaver.resetOptions();
							arffsaver.setInstances(copy);
							arffsaver.setFile(new File(fileName));
							arffsaver.writeBatch();
							CsvWriter arffwriter = new CsvWriter(new FileWriter(fileName, true),
									',');
							arffWriters.put("AGENT" + i, arffwriter);
						}

						logger.fine("AGENT" + i + " dataset created in csv and arff formats.");
					}
					// Append essentials to all
					for (String[] essential : essentials) {
						for (CsvWriter wr : writers.values()) {
							wr.writeRecord(essential);
						}
						for (CsvWriter arffwr : arffWriters.values()) {
							arffwr.writeRecord(essential);
						}
					}

					int agentCounter = 0;
					for (int j = 0; j < trainData.numInstances(); j++) {
						Instance instance = trainData.instance(j);
						CsvWriter writer = writers.get("AGENT" + agentCounter);
						CsvWriter arffwriter = arffWriters.get("AGENT" + agentCounter);
						String[] row = new String[instance.numAttributes()];
						for (int a = 0; a < instance.numAttributes(); a++) {
							row[a] = instance.stringValue(a);
						}
						if (writer != null) {
							writer.writeRecord(row);
						}
						if (arffwriter != null) {
							arffwriter.writeRecord(row);
						}
						agentCounter++;
						if (agentCounter == agents) {
							agentCounter = 0;
						}
					}

					for (CsvWriter wr : writers.values()) {
						wr.close();
					}
					for (CsvWriter arffwr : arffWriters.values()) {
						arffwr.close();
					}
				}

			} catch (Exception e) {
				logger.severe("Exception while splitting dataset. ->");
				logger.severe(e.getMessage());
				System.exit(1);
			}

			logger.finest("Dataset for fold " + fold + " created.");
		}

		logger.finer("<-- splitDataset()");

	}

	/**
	 * @param ratio
	 * @param agents
	 * @param originalDatasetPath
	 * @param outputDir
	 * @param central
	 * @param scenario
	 */
	private void createExperimentInfoFile(double ratio, int agents, String originalDatasetPath,
			String outputDir, boolean central, String scenario, Logger logger) {

		try {
			String fileName = outputDir + "/" + agents + "agents/experiment.info";
			File file = new File(fileName);
			File parent = file.getParentFile();
			if (!parent.exists()) {
				parent.mkdirs();
			}
			FileWriter fw = new FileWriter(file);
			fw.write("Scenario: " + scenario + "\n");
			fw.write("Test Cases Ratio: " + Double.toString(ratio) + "\n");
			fw.write("Number of Agents: " + Integer.toString(agents) + "\n");
			fw.write("Original dataset: " + originalDatasetPath + "\n");
			fw.write("Experiment dataset folder: " + outputDir + "\n");
			fw.write("Central approach: " + Boolean.toString(central) + "\n");
			fw.close();

		} catch (Exception e) {
			logger.severe(e.getMessage());
			System.exit(1);
		}
	}

	/**
	 * @param originalDatasetPath
	 * @param scenario
	 * @return
	 */
	private List<String[]> getEssentials(String originalDatasetPath, Logger logger) {
		// Find essentials
		List<String[]> essentials = new ArrayList<String[]>();
		HashMap<String, List<String>> nodesAndStates = new HashMap<String, List<String>>();
		try {
			// Look for all possible states
			Reader fr = new FileReader(originalDatasetPath);
			CsvReader reader = new CsvReader(fr);
			reader.readHeaders();
			String[] headers = reader.getHeaders();
			for (String header : headers) {
				nodesAndStates.put(header, new ArrayList<String>());
			}
			String[] values;
			while (reader.readRecord()) {
				values = reader.getValues();
				for (int i = 0; i < values.length; i++) {
					if (!nodesAndStates.get(headers[i]).contains(values[i])) {
						nodesAndStates.get(headers[i]).add(values[i]);
						if (!essentials.contains(values)) {
							essentials.add(values);
						}
					}
				}
			}

			reader.close();

			logger.fine("Number of Essentials: " + essentials.size());
		} catch (Exception e) {
			logger.severe(e.getMessage());
			System.exit(1);
		}
		return essentials;
	}

	/**
	 * @param ratio
	 * @param agents
	 * @param originalDatasetPath
	 * @param outputDir
	 * @param central
	 * @param scenario
	 */
	private void createExperimentInfoFile(int folds, int agents, String originalDatasetPath,
			String outputDir, String scenario, Logger logger) {

		try {
			String fileName = outputDir + "/" + agents + "agents/experiment.info";
			File file = new File(fileName);
			File parent = file.getParentFile();
			if (!parent.exists()) {
				parent.mkdirs();
			}
			if (!file.exists()) {
				FileWriter fw = new FileWriter(file);
				fw.write("Scenario: " + scenario + "\n");
				fw.write("Number of folds: " + Integer.toString(folds) + "\n");
				fw.write("Number of Agents: " + Integer.toString(agents) + "\n");
				fw.write("Original dataset: " + originalDatasetPath + "\n");
				fw.write("Experiment dataset folder: " + outputDir + "\n");
				fw.close();
			}

		} catch (Exception e) {
			logger.severe(e.getMessage());
			System.exit(1);
		}
	}

	/**
	 * @param csvFilePath
	 * @return
	 * @throws Exception
	 */
	private Instances getDataFromCSV(String csvFilePath) throws Exception {
		DataSource source = new DataSource(csvFilePath);
		Instances data = source.getDataSet();
		data.setClassIndex(data.numAttributes() - 1);
		return data;
	}

	private void copyFileUsingApacheCommonsIO(File source, File dest, Logger logger) {
		try {
			FileUtils.copyFile(source, dest);
		} catch (IOException e) {
			logger.severe("Problems copying file noEssentials");
			e.printStackTrace();
			System.exit(1);
		}
	}
}
