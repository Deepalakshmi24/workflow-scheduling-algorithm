package workflow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TempWTask implements Serializable
{
	private final String taskId;  //�����ID
	private int taskWorkFlowId;   //�������ڹ�������ID
	private final double runTime; //�����ִ��ʱ�� 
	
	private List<Constraint> parentTaskList;   //���ڵ��б�
	private List<Constraint> sucessorTaskList; //�ӽڵ��б�
	
	public TempWTask(String taskId, int workflowId, double runTime)
	{	
		this.taskId = taskId;
		this.taskWorkFlowId = workflowId;
		this.runTime = runTime;				
		parentTaskList = new ArrayList<Constraint>();   //��ʼ�����ڵ��б�
		sucessorTaskList = new ArrayList<Constraint>(); //��ʼ���ӽڵ��б�
	}
	
	//�����id
	public String getTaskId(){return taskId;}
	
	//�������ڹ�������Id
	public int getTaskWorkFlowId(){return taskWorkFlowId;}
	public void setTaskWorkFlowId(int workFlowId)
	{
		this.taskWorkFlowId = workFlowId;
	}
	
	//�����ִ��ʱ��
	public double getTaskRunTime(){return runTime;}
	
	//��ȡ������ĸ������б�
	public List<Constraint> getParentTaskList(){return parentTaskList;}
	
	//��ȡ��������������б�
	public List<Constraint> getSuccessorTaskList(){return sucessorTaskList;}
}
