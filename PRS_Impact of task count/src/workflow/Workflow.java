package workflow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Workflow implements Serializable, Cloneable
{
	private int workflowId;
	private final String workflowName;
	private int arrivalTime;
	private double makespan;
	private int deadline;
	
	private List<WTask> taskList; //节点列表
	private boolean startedFlag;  //工作流是否已经开始放置
	private double finishTime;       //工作流的完成时间  int
	private boolean successfulOrNot; //工作流是否被成功执行
	
	public Workflow(int workFlowId, String name, int arrivalTime, double makespan, int deadline)
	{
		this.workflowId = workFlowId;
		this.workflowName = name;
		this.arrivalTime = arrivalTime;
		this.makespan = makespan;
		this.deadline = deadline;
		
		this.taskList = new ArrayList<WTask>(); //初始化任务列表
		this.startedFlag = false;
		this.finishTime = -1;
		this.successfulOrNot = false;
	}
		
	/**获取工作流的ID*/
	public int getWorkflowId(){return workflowId;}
	/**设置工作流的ID*/
	public void setWorkflowId(int workflowId)
	{
		this.workflowId = workflowId;
	}
	
	/**获取工作流的名字*/
	public String getWorkflowName(){return workflowName;}
	
	/**获取工作流的到达时间*/
	public int getArrivalTime(){return arrivalTime;}
	/**设置工作流的到达时间*/
	public void setArrivalTime(int arrivalTime)
	{
		this.arrivalTime = arrivalTime;
	}
	
	/**获取工作流的跨期*/
	public double getMakespan(){return makespan;}
	/**设置工作流的跨期*/
	public void setMakespan(double makespan)
	{
		this.makespan = makespan;
	}
	
	/**获取工作流的截止期*/
	public int getDeadline(){return deadline;}
	/**设置工作流的截止期*/
	public void setDeadline(int deadline)
	{
		this.deadline = deadline;
	}
	
	/**获取该工作流的任务集合*/
	public List<WTask> getTaskList(){return taskList;}
	/**设置该工作流的任务集合*/
	public void setTaskList(List<WTask> list)
	{
		this.taskList = list;
	}
	
	/**获取工作流已经开始放置的标志*/
	public boolean getStartedFlag(){return startedFlag;}
	/**设置工作流已经开始放置的标志*/
	public void setStartedFlag(boolean startedFlag)
	{
		this.startedFlag = startedFlag;
	}
	
	/**获取工作流的完成时间*/
	public double getFinishTime(){return finishTime;}
	/**设置工作流的完成时间*/
	public void setFinishTime(double finishTime)
	{
		this.finishTime = finishTime;
	}
	
	/**获取工作流是否在截止期内完成*/
	public boolean getSuccessfulOrNot(){return successfulOrNot;}
	/**设置工作流是否在截止期内完成*/
	public void setSuccessfulOrNot(boolean successfulOrNot)
	{
		this.successfulOrNot = successfulOrNot;
	}
}