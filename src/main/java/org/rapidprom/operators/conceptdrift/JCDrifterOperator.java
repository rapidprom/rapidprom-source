package org.rapidprom.operators.conceptdrift;

import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apromore.prodrift.driftdetector.ControlFlowDriftDetector;
import org.apromore.prodrift.model.ProDriftDetectionResult;
import org.deckfour.xes.model.XLog;
import org.processmining.conceptdrift.test.SuddenConceptDriftTest;
import org.rapidprom.ioobjects.XLogIOObject;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.io.AbstractDataReader.AttributeColumn;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;

public class JCDrifterOperator extends Operator {

	public static final String PARAMETER_1_KEY = "Method:", PARAMETER_1_DESCR = "";

	public static final String SUDDEN = "sudden", GRADUAL = "gradual";

	private InputPort input = getInputPorts().createPort("event log", XLogIOObject.class);
	private OutputPort output = getOutputPorts().createPort("drift points");

	public JCDrifterOperator(OperatorDescription description) {
		super(description);
		// TODO Auto-generated constructor stub
		ExampleSetMetaData md1 = new ExampleSetMetaData();
		AttributeMetaData amd1 = new AttributeMetaData("Drift ID", Ontology.NUMERICAL);
		amd1.setRole(AttributeColumn.REGULAR);
		amd1.setNumberOfMissingValues(new MDInteger(0));
		md1.addAttribute(amd1);
		AttributeMetaData amd4 = new AttributeMetaData("Time (ms)", Ontology.NUMERICAL);
		amd4.setRole(AttributeColumn.REGULAR);
		amd4.setNumberOfMissingValues(new MDInteger(0));
		md1.addAttribute(amd4);

		getTransformer().addRule(new GenerateNewMDRule(output, md1));
	}

	public void doWork() throws OperatorException {
		Logger logger = LogService.getRoot();
		logger.log(Level.INFO, "Start: detecting concept drift (JC)");
		long time = System.currentTimeMillis();

		XLogIOObject log = input.getData(XLogIOObject.class);
		List<Date> driftDateList = null;

		if (SUDDEN.equals(getParameterAsString(PARAMETER_1_KEY))) {
			SuddenConceptDriftTest s = new SuddenConceptDriftTest();
			driftDateList = s.findConceptDrifts(log.getArtifact());
			fillDriftPoints(driftDateList);
		}

		Collections.sort(driftDateList);
		logger.log(Level.INFO,
				"End: detecting concept drift (JC) (" + (System.currentTimeMillis() - time) / 1000 + " sec)");
	}

	private void fillDriftPoints(List<Date> drifts) {
		ExampleSet es = null;
		MemoryExampleTable table = null;
		List<Attribute> attributes = new LinkedList<Attribute>();
		attributes.add(AttributeFactory.createAttribute("Drift ID", Ontology.NUMERICAL));
		attributes.add(AttributeFactory.createAttribute("Time (ms)", Ontology.NUMERICAL));
		table = new MemoryExampleTable(attributes);

		if (drifts != null) {
			int traceID = 0;
			Iterator<Date> iterator = drifts.iterator();
			DataRowFactory factory = new DataRowFactory(DataRowFactory.TYPE_DOUBLE_ARRAY, '.');

			while (iterator.hasNext()) {
				Date next = iterator.next();
				Object[] vals = new Object[2];
				vals[0] = traceID;
				vals[1] = next.getTime();

				DataRow dataRow = factory.create(vals, attributes.toArray(new Attribute[attributes.size()]));
				table.addDataRow(dataRow);
				traceID++;
			}
		}

		es = table.createExampleSet();
		output.deliver(es);
	}
	
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> parameterTypes = super.getParameterTypes();

		ParameterTypeCategory par1 = new ParameterTypeCategory(PARAMETER_1_KEY, PARAMETER_1_DESCR,
				new String[] { SUDDEN, GRADUAL }, 0);
		parameterTypes.add(par1);

		return parameterTypes;
	}

}