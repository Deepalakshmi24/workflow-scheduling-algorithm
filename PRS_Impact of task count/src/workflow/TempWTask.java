package workflow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TempWTask implements Serializable
{
	private final String taskId;  //任务的ID
	private int taskWorkFlowId;   //任务所在工作流的ID
	private final double runTime; //任务的执行时间 
	
	private List<Constraint> parentTaskList;   //父节点列表
	private List<Constraint> sucessorTaskList; //子节点列表
	
	public TempWTask(String taskId, int workflowId, double runTime)
	{	
		this.taskId = taskId;
		this.taskWorkFlowId = workflowId;
		this.runTime = runTime;				
		parentTaskList = new ArrayList<Constraint>();   //初始化父节点列表
		sucessorTaskList = new ArrayList<Constraint>(); //初始化子节点列表
	}
	
	//任务的id
	public String getTaskId(){return taskId;}
	
	//任务所在工作流的Id
	public int getTaskWorkFlowId(){return taskWorkFlowId;}
	public void setTaskWorkFlowId(int workFlowId)
	{
		this.taskWorkFlowId = workFlowId;
	}
	
	//任务的执行时间
	public double getTaskRunTime(){return runTime;}
	
	//获取该任务的父任务列表
	public List<Constraint> getParentTaskList(){return parentTaskList;}
	
	//获取该任务的子任务列表
	public List<Constraint> getSuccessorTaskList(){return sucessorTaskList;}
}
