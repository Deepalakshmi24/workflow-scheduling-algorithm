package workflow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TempWorkflow implements Serializable
{
	private final String workflowName;	
	private List<TempWTask> taskList; //�ڵ��б�
	
	public TempWorkflow(String name)
	{
		this.workflowName = name;		
		this.taskList = new ArrayList<TempWTask>(); //��ʼ�������б�
	}
	
	//������������
	public String getWorkflowName(){return workflowName;}
	
	//��ȡ�ù����������񼯺�
	public List<TempWTask> getTaskList(){return taskList;}
	public void setTaskList(List<TempWTask> list)
	{
		this.taskList = list;
	}
}
