package main;

import share.StaticfinalTags;
import workflow.WorkflowProducer;

public class AutoTest 
{
	public static void main(String[] args) throws Exception 
	{
		for(int i=1; i<15; i++) //一共15个模板
		{
			StaticfinalTags.selectedNum = i;
			WorkflowProducer.main(null); //产生工作流
//			StaticfinalTags.initialVmNum = i;
			System.out.println("Vm Num: "+StaticfinalTags.initialVmNum);
			WorkflowExperiment.main(null);
		}
	}

}
