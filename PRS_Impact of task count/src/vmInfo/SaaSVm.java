package vmInfo;

import java.util.ArrayList;
import java.util.List;
import workflow.WTask;

public class SaaSVm 
{
	private final int vmID; //虚拟机的ID
	private final String vmType; //虚拟机的类型
	
	private final double vmPrice; //虚拟机的价格
	private double totalCost; //虚拟机生命周期内的费用
	
	private final double startWorkTime; //虚拟机开始工作的时间
	private double endWorkTime; //虚拟机结束工作的时间
	private double realWorkTime; //虚拟机实际工作的时间
	private double idleTime; //虚拟机空闲的时间
	
	private final double executionTimeFactor; //任务执行时间的影响因子
	private double finishTime; //完成正在执行任务的时间
	private double realFinishTime; //真正的完成时间
	private double readyTime; //虚拟机就绪时间
	
	private boolean status; //虚拟机状态
		
	private List<WTask> WTaskList; //虚拟机完成的所有任务	
	private List<WTask> waitWTaskList; //等待任务列表	
	private WTask waitingWTask; //正在等待的任务     
	private WTask executingWTask; //正在执行任务
	
	private List<CommPort> commPortList; //虚拟机的通信接口
	
	public SaaSVm(int id, String type, double startTime, 
			double price, double factor, List<CommPort> list)
	{
		this.vmID = id;
		this.vmType = type;
		this.vmPrice = price;
		this.executionTimeFactor = factor;
		
		this.totalCost = 0;
		this.startWorkTime = startTime;
		this.endWorkTime = startTime + 3600; //时间以秒为单位
		this.realWorkTime = 0;
		this.idleTime = 0;
		
		this.finishTime = -1;
		this.realFinishTime = -1;
		this.readyTime = startTime; //刚启动虚拟机时，虚拟机的就绪时间就是开始工作时间
		
		this.status = true;
		this.WTaskList = new ArrayList<WTask>();
		this.waitWTaskList = new ArrayList<WTask>();
		this.waitingWTask = new WTask();
		this.executingWTask = new WTask();
		
		this.commPortList = list;
	}
	
	/**获取虚拟机的通信接口*/
	public List<CommPort> getCommPortList(){return commPortList;}
	
	/**获取虚拟机的ID*/
	public int getVmID(){return vmID;}
	
	/**获取虚拟机的类型*/
	public String getVmType(){return vmType;}
	
	/**获取虚拟机的价格*/
	public double getVmPrice(){return vmPrice;}		
	
	/**获取虚拟机对任务执行时间的影响因素*/
	public double getVmFactor(){return executionTimeFactor;}
	
	/**获取虚拟机开始工作的时间*/
	public double getVmStartWorkTime(){return startWorkTime;}
	
	/**获取虚拟机的总费用*/
	public double getTotalCost(){return totalCost;}
	/**更新虚拟机的总费用*/
	public void setTotalCost(double add)
	{
		this.totalCost = add;
	}
		
	/**获取虚拟机的结束时间*/
	public double getEndWorkTime(){return endWorkTime;}
	/**设置虚拟机的结束时间*/
	public void setEndWorkTime(double endTime)
	{
		this.endWorkTime = endTime;
	}
	
	/**获取虚拟机真正的工作时间*/
	public double getRealWorkTime(){return realWorkTime;}
	/**设置虚拟机真正的工作时间*/
	public void updateRealWorkTime(double workTime)
	{
		this.realWorkTime += workTime;
	}
	
	/**获取虚拟机的空闲时间*/
	public double getIdleTime(){return idleTime;}
	/**更新虚拟机的空闲时间*/
	public void updateIdleTime(double idleTime)
	{
		this.idleTime += idleTime;
	}
	
	/**获取虚拟机完成任务的期望时间*/
	public double getFinishTime(){return finishTime;}
	/**设置虚拟机完成任务的期望时间*/
	public void setFinishTime(double finishTime)
	{
		this.finishTime = finishTime;
	}
	
	/**获取虚拟机完成任务的真正时间*/
	public double getRealFinishTime(){return realFinishTime;}
	/**设置虚拟机完成任务的真正时间*/
	public void setRealFinishTime(double rFinishTime)
	{
		this.realFinishTime = rFinishTime;
	}
	
	/**获取虚拟机的就绪时间*/
	public double getReadyTime(){return readyTime;}
	/**设置虚拟机的就绪时间*/
	public void setReadyTime(double readyTime)
	{
		this.readyTime = readyTime;
	}
	
	/**获取虚拟机的状态*/
	public boolean getVmStatus(){return status;}
	/**设置虚拟机的状态*/
	public void setVmStatus(boolean status)
	{
		this.status = status;
	}
	
	/**获取虚拟机所有完成任务的列表*/
	public List<WTask> getWTaskList(){return WTaskList;}
	
	/**获取虚拟机上正在等待的任务列表*/
	public List<WTask> getWaitWTaskList(){return waitWTaskList;}
	
	/**获取虚拟机上正在等待的任务*/
	public WTask getWaitingWTask(){return waitingWTask;}
	/**设置虚拟机上正在等待的任务*/
	public void setWaitingWTask(WTask task)
	{
		this.waitingWTask = task;
	}
	
	/**获取虚拟机上正在执行的任务*/
	public WTask getExecutingWTask(){return executingWTask;}
	/**设置虚拟机上正在执行的任务*/
	public void setExecutingWTask(WTask task)
	{
		this.executingWTask = task;
	}
		
}
