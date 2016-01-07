package workflow;

import java.io.Serializable;

/**直接使用任务实例*/
public class ConstraintWTask implements Serializable
{
	private final WTask wTask; //连接任务
	private final int dataSize; //数据传输量
	
	/**直接使用任务实例的前后约束关系*/
	public ConstraintWTask(WTask task, int dataSize)
	{
		this.wTask = task;
		this.dataSize = dataSize;
	}
	
	public WTask getWTask(){return wTask;}	
	public int getDataSize(){return dataSize;}
}
