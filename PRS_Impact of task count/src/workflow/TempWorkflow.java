package workflow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TempWorkflow implements Serializable
{
	private final String workflowName;	
	private List<TempWTask> taskList; //节点列表
	
	public TempWorkflow(String name)
	{
		this.workflowName = name;		
		this.taskList = new ArrayList<TempWTask>(); //初始化任务列表
	}
	
	//工作流的名字
	public String getWorkflowName(){return workflowName;}
	
	//获取该工作流的任务集合
	public List<TempWTask> getTaskList(){return taskList;}
	public void setTaskList(List<TempWTask> list)
	{
		this.taskList = list;
	}
}
