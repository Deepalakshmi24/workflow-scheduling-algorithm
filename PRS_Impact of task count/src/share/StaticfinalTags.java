package share;

/**
 * 统一参数值，比如：主机、虚拟机开机时间。
 * @author  hkchen--2014.10.12
 */
public final class StaticfinalTags 
{
	/**算法的选择*/
	public static int choose = 0;
	/**工作流的数量*/	
	public static int workflowNum = 1000;
	/**工作流的到达率*/
	public static double arrivalLamda = 0.1;
	/**工作流截止期的基准*/
	public static double deadlineBase = 1.5;
	/**执行时间方差的系数*/
	public static double standardDeviation = 0.2;
	/**可信度*/
	public static double confidency = 0.85; //修改为0.8
	/**当前时间, 如果为负数，则无效*/
	public static double currentTime = -1;
	/**初始化时,虚拟机的数量*/
	public static int initialVmNum = 3;
	/**创建虚拟机的时间开销*/
	public static int createVmTime = 30;
	/**选择*/
	public static int selectedNum = 9; //	
	/**PRS中任务执行时间的可信度*/
//	public static double PRSConfidency = 0.8;
	/**带宽*/
	public static double bandwidth = 1000000;
	/**接口的数量*/
	public final static int portNum = 6;
	/**虚拟机间最大允许的空闲时间*/
	public static int maxIdleTime = 3600;
}
