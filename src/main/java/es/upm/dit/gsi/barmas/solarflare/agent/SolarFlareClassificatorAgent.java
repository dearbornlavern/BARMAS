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
 * es.upm.dit.gsi.barmas.agent.BarmasAgent.java
 */
package es.upm.dit.gsi.barmas.solarflare.agent;

import jason.asSemantics.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import unbbayes.prs.bn.ProbabilisticNetwork;
import es.upm.dit.gsi.barmas.agent.capability.argumentation.AgentArgumentativeCapability;
import es.upm.dit.gsi.barmas.agent.capability.argumentation.ArgumentativeAgent;
import es.upm.dit.gsi.barmas.agent.capability.argumentation.bayes.Argument;
import es.upm.dit.gsi.barmas.agent.capability.argumentation.manager.ArgumentationManagerAgent;
import es.upm.dit.gsi.barmas.solarflare.model.SolarFlare;
import es.upm.dit.gsi.barmas.solarflare.model.scenario.SolarFlareScenario;
import es.upm.dit.gsi.barmas.solarflare.model.vocabulary.SolarFlareType;
import es.upm.dit.gsi.barmas.solarflare.simulation.SolarFlareClassificationSimulation;
import es.upm.dit.gsi.shanks.ShanksSimulation;
import es.upm.dit.gsi.shanks.agent.SimpleShanksAgent;
import es.upm.dit.gsi.shanks.agent.capability.reasoning.bayes.BayesianReasonerShanksAgent;
import es.upm.dit.gsi.shanks.agent.capability.reasoning.bayes.ShanksAgentBayesianReasoningCapability;
import es.upm.dit.gsi.shanks.exception.ShanksException;

/**
 * Project: barmas File: es.upm.dit.gsi.barmas.agent.BarmasAgent.java
 * 
 * Grupo de Sistemas Inteligentes Departamento de Ingeniería de Sistemas
 * Telemáticos Universidad Politécnica de Madrid (UPM)
 * 
 * @author alvarocarrera
 * @email a.carrera@gsi.dit.upm.es
 * @twitter @alvarocarrera
 * @date 23/07/2013
 * @version 0.1
 * 
 */
public class SolarFlareClassificatorAgent extends SimpleShanksAgent implements
		BayesianReasonerShanksAgent, ArgumentativeAgent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8582918551821278046L;

	private ArgumentationManagerAgent manager;
	private String bnFilePath;
	private ProbabilisticNetwork bn;
	private List<String> sensors;

	/**
	 * Constructor
	 * 
	 * @param id
	 * @param manager
	 * @param bnPath
	 */
	public SolarFlareClassificatorAgent(String id,
			ArgumentationManagerAgent manager, String bnPath,
			List<String> sensors) {
		super(id);
		this.bnFilePath = bnPath;
		this.sensors = sensors;
		this.setArgumentationManager(manager);
		try {
			ShanksAgentBayesianReasoningCapability.loadNetwork(this);
		} catch (ShanksException e) {
			e.printStackTrace();
		}
		
		//Register in manager
		this.getArgumentationManager().addSubscriber(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see es.upm.dit.gsi.shanks.agent.ShanksAgent#checkMail()
	 */
	public void checkMail() {
		// Check inbox
		List<Message> inbox = this.getInbox();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * es.upm.dit.gsi.shanks.agent.SimpleShanksAgent#executeReasoningCycle(es
	 * .upm.dit.gsi.shanks.ShanksSimulation)
	 */
	@Override
	public void executeReasoningCycle(ShanksSimulation simulation) {
		// Process incoming messages

		// Check sensors
		SolarFlareClassificationSimulation sim = (SolarFlareClassificationSimulation) simulation;
		SolarFlare orig = (SolarFlare) sim.getScenario().getNetworkElement(
				SolarFlareScenario.ORIGINALFLARE);

		HashMap<String, String> evidences = new HashMap<String, String>();

		for (String sensor : this.sensors) {
			String value = (String) orig.getProperty(sensor);
			evidences.put(sensor, value);
		}

		try {
			ShanksAgentBayesianReasoningCapability
					.addEvidences(this, evidences);
			// Get hypothesis
			HashMap<String, Float> hyps = ShanksAgentBayesianReasoningCapability
					.getNodeStatesHypotheses(this,
							SolarFlareType.class.getSimpleName());
			String hyp = "";
			float maxValue = 0;
			for (Entry<String, Float> entry : hyps.entrySet()) {
				if (entry.getValue() > maxValue) {
					maxValue = entry.getValue();
					hyp = entry.getKey();
				}
			}
			
			// Create and send initial argument
			Argument arg = AgentArgumentativeCapability.createArgument(this, SolarFlareType.class.getSimpleName(), hyp, maxValue, evidences);
			AgentArgumentativeCapability.sendArgument(this, arg);
			
		} catch (ShanksException e) {
			e.printStackTrace();
		}
	}

	public String getProponentName() {
		return this.getID();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * es.upm.dit.gsi.barmas.agent.capability.argumentation.ArgumentativeAgent
	 * #getProponent()
	 */
	public ArgumentativeAgent getProponent() {
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * es.upm.dit.gsi.barmas.agent.capability.argumentation.ArgumentativeAgent
	 * #getArgumentationManager()
	 */
	public ArgumentationManagerAgent getArgumentationManager() {
		return this.manager;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * es.upm.dit.gsi.barmas.agent.capability.argumentation.ArgumentativeAgent
	 * #getArgumentationManagerName()
	 */
	public String getArgumentationManagerName() {
		SimpleShanksAgent ag = (SimpleShanksAgent) this.manager;
		return ag.getID();
	}

	public Set<Argument> getCurrentArguments() throws ShanksException {
		// TODO Auto-generated method stub
		return null;
	}

	public void updateBeliefsWithNewArguments(Set<Argument> args)
			throws ShanksException {
		// TODO Auto-generated method stub

	}

	public ProbabilisticNetwork getBayesianNetwork() {
		return this.bn;
	}

	public void setBayesianNetwork(ProbabilisticNetwork bn) {
		this.bn = bn;
	}

	public String getBayesianNetworkFilePath() {
		return this.bnFilePath;
	}

	public void setArgumentationManager(ArgumentationManagerAgent manager) {
		this.manager = manager;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * es.upm.dit.gsi.barmas.agent.capability.argumentation.ArgumentativeAgent
	 * #sendArgument
	 * (es.upm.dit.gsi.barmas.agent.capability.argumentation.bayes.Argument)
	 */
	public void sendArgument(Argument arg) {
		Message m = new Message();
		m.setPropCont(arg);
		m.setReceiver(this.getArgumentationManagerName());
		super.sendMsg(m);
	}
}
