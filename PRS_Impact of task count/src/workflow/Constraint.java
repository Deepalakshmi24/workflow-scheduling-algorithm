package workflow;

import java.io.Serializable;

//chk 2014.10.18 ChongQing
public class Constraint implements Serializable
{
	private final String taskId; //连接任务
	private final int dataSize; //数据传输量
	
	public Constraint(String taskId, int dataSize)
	{
		this.taskId = taskId;
		this.dataSize = dataSize;
	}
	
	public String getTaskId(){return taskId;}	
	public int getDataSize(){return dataSize;}
}
