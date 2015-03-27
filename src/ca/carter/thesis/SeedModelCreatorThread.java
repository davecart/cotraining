package ca.carter.thesis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

import ca.carter.thesis.ml.ClassWeighting;
import ca.carter.thesis.ml.ModelType;
import ca.carter.thesis.ml.SVMTokenModel;
import ca.carter.thesis.ml.SVMTokenModelFeature;
import ca.carter.thesis.ml.SVMTokenModelSentiment;
import ca.carter.thesis.model.Task;
import ca.carter.thesis.model.TokenWithContext;

public class SeedModelCreatorThread extends Thread {

	private List<SVMTokenModel> models;
	private String modelFileOutput;
	private String fileName;
	private ModelType modelType;
	private Task task;
	private List<TokenWithContext> seedTokens;
	private ClassWeighting classWeighting;
	private Double c;
	private Double gamma;
	private Double epsilon;
	
	public SeedModelCreatorThread(List<SVMTokenModel> models, String modelFileOutput, String fileName,
			ModelType modelType, Task task, List<TokenWithContext> seedTokens, ClassWeighting classWeighting, Double c, Double gamma, Double epsilon) {
		super();
		this.models = models;
		this.modelFileOutput = modelFileOutput;
		this.fileName = fileName;
		this.modelType = modelType;
		this.task = task;
		this.seedTokens = seedTokens;
		this.classWeighting = classWeighting;
		this.c = c;
		this.gamma = gamma;
		this.epsilon = epsilon;
	}



	public void run() {
		Writer[] modelWriter = null;
		if (modelFileOutput != null)
		{
			try {
				modelWriter = new Writer[2];
				modelWriter[0] = new BufferedWriter(new FileWriter(modelFileOutput + "view0lexical" + fileName));
				modelWriter[1] = new BufferedWriter(new FileWriter(modelFileOutput + "view1syntactic" + fileName));				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		SVMTokenModel model = null;
		switch (modelType)
		{
		case FEATURE:
			model = new SVMTokenModelFeature(task, seedTokens, modelWriter, classWeighting, c, gamma, epsilon);
			break;
		case SENTIMENT:
			model = new SVMTokenModelSentiment(task, seedTokens, modelWriter, classWeighting, c, gamma, epsilon);
			break;
		}
		
		System.out.println(modelType + ": C is " + model.getC(0) + ", gamma is " + model.getGamma(0) + ", and epsilon is " + model.getEpsilon() + "; using " + classWeighting + " weighting policy." );
		models.add(model);

		
	}
}
