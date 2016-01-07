package main;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import share.StaticfinalTags;
import workflow.Workflow;
import workflowScheduleAlgorithm.PCP_RTC_CompareAlgorithm;
import workflowScheduleAlgorithm.SHEFT_CompareAlgorithm;
import workflowScheduleAlgorithm.ScheduleAlgorithms;

public class WorkflowExperiment 
{
	private static List<Workflow> workflowList; //����������
	private static int workflowNum = 0; //ʵ���й�����������
	
	public static void main(String[] args) throws Exception 
	{
		workflowNum = StaticfinalTags.workflowNum;
		workflowList = getWorkflowListFromFile("producedWorkflow.txt");//��ȡ���������Լ�
				
		if(StaticfinalTags.choose == 0)
		{
//			StaticfinalTags.PRSConfidency = 0.85;
			StaticfinalTags.confidency = 0.85;
			ScheduleAlgorithms algorithm = new ScheduleAlgorithms();
			algorithm.submitWorkflowList(workflowList); //�ύ����������������		
			algorithm.scheduleDynamicWorkflowToSaaSVmByPRS(); //�����㷨��ʵ��	
			
			workflowList.clear();
			Runtime.getRuntime().gc();
		}
		else if(StaticfinalTags.choose == 1)
		{
			SHEFT_CompareAlgorithm algorithm = new SHEFT_CompareAlgorithm();
			algorithm.submitWorkflowList(workflowList); //�ύ����������������		
			algorithm.scheduleDynamicWorkflowsBySHEFT(); //�����㷨��ʵ��
			
			workflowList.clear();
			Runtime.getRuntime().gc();
		}
		else if(StaticfinalTags.choose == 2)
		{
			PCP_RTC_CompareAlgorithm algorithm = new PCP_RTC_CompareAlgorithm();
			algorithm.submitWorkflowList(workflowList); //�ύ����������������		
			algorithm.scheduleDynamicWorkflowsByRTC(); //�����㷨��ʵ��	
			
			workflowList.clear();
			Runtime.getRuntime().gc();
		}								
	}
	
	/**��ȡ���������Լ�
	 * @throws IOException 
	 * @throws ClassNotFoundException */
	public static List<Workflow> getWorkflowListFromFile(String filename) throws IOException, ClassNotFoundException
	{
		List<Workflow> w_List = new ArrayList<Workflow>();
		Workflow w = null;
		FileInputStream fi = new FileInputStream(filename);
		ObjectInputStream si = new ObjectInputStream(fi);
		try
		{
			for(int i=0; i<workflowNum; i++)
			{
				w = (Workflow)si.readObject();
				w_List.add(w);
			}			
			si.close();
		}catch(IOException e){System.out.println(e.getMessage());}		
		return w_List;
	}
}
