package main;

import share.StaticfinalTags;
import workflow.WorkflowProducer;

public class AutoTest 
{
	public static void main(String[] args) throws Exception 
	{
		for(int i=1; i<15; i++) //һ��15��ģ��
		{
			StaticfinalTags.selectedNum = i;
			WorkflowProducer.main(null); //����������
//			StaticfinalTags.initialVmNum = i;
			System.out.println("Vm Num: "+StaticfinalTags.initialVmNum);
			WorkflowExperiment.main(null);
		}
	}

}
