package workflow;

import java.io.Serializable;

/**ֱ��ʹ������ʵ��*/
public class ConstraintWTask implements Serializable
{
	private final WTask wTask; //��������
	private final int dataSize; //���ݴ�����
	
	/**ֱ��ʹ������ʵ����ǰ��Լ����ϵ*/
	public ConstraintWTask(WTask task, int dataSize)
	{
		this.wTask = task;
		this.dataSize = dataSize;
	}
	
	public WTask getWTask(){return wTask;}	
	public int getDataSize(){return dataSize;}
}
