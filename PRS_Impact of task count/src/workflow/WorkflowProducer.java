package workflow;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import share.StaticfinalTags;

public class WorkflowProducer 
{
	private static List<Workflow> workflowList; //工作流集合
	
	public static void main(String[] args) throws ClassNotFoundException, IOException 
	{
		workflowList = new ArrayList<Workflow>(); //初始化工作流列表
		int workflowNum = StaticfinalTags.workflowNum; //加一个扫尾的工作流
		workflowList = produceWorkflow(workflowNum); //产生工作流测试集		
//		System.out.println("Num: "+workflowList.size());
		
		calculateRealTaskBaseExecutionTime(workflowList); //赋值于所有工作流中每个任务的随机参数
		
		//将工作流写入文件tempWorkflow.txt中,以供对比试验使用
		FileOutputStream fos = new FileOutputStream("producedWorkflow.txt"); 
		ObjectOutputStream os = new ObjectOutputStream(fos);
		try
		{
			for(int i=0; i<workflowList.size(); i++)
			{
//				System.out.println("WriteNum: "+i);
				os.writeObject(workflowList.get(i));
			}
			os.close();
		}catch(IOException e){System.out.println(e.getMessage());}
		workflowList.clear();
//		System.out.println("End Write!");
	}// end main()
	
	/**根据工作流模板，产生工作流测试集*/
	private static List<Workflow> produceWorkflow(int workflowNum) throws IOException, ClassNotFoundException
	{	
//		System.out.println("Start produce workflow!");
		//读取样本工作流
		List<Workflow> templateWList = new ArrayList<Workflow>();
		FileInputStream fi = new FileInputStream("tempWorkflow.txt");
		ObjectInputStream si = new ObjectInputStream(fi);
		try
		{
			for(int i=0; i<15; i++)
			{
				TempWorkflow readWorkflow = (TempWorkflow)si.readObject();
				
				List<WTask> taskList = new ArrayList<WTask>(); //工作流的任务集合
				List<TempWTask> tempWTaskList = readWorkflow.getTaskList();//模板工作流中的任务集合	
				for(TempWTask task: tempWTaskList)
				{//复制工作流readWorkflow中的每个任务
					String taskId = task.getTaskId(); //任务的ID
					int workflowId = -1; //初始化任务所在工作流的ID
					double baseExecutionTime =  task.getTaskRunTime(); //任务的基准执行时间
					if(baseExecutionTime < 0)
					{
						throw new IllegalArgumentException("Task base execution time is less than zero!");
//						baseExecutionTime = 1;
//						System.out.println("task exeuction time is less than one second");
					}
					
					//设置父任务集合
					List<Constraint> parentConstraintList = new ArrayList<Constraint>();
					for(Constraint con: task.getParentTaskList())
					{
						String parentTaskId = con.getTaskId();
						int dataSize = con.getDataSize();
						Constraint tempParentConstraint = new Constraint(parentTaskId, dataSize);
						parentConstraintList.add(tempParentConstraint);
					}
					
					//子任务集合
					List<Constraint> successorConstraintList = new ArrayList<Constraint>();
					for(Constraint con: task.getSuccessorTaskList())
					{
						String parentTaskId = con.getTaskId();
						int dataSize = con.getDataSize();
						Constraint tempParentConstraint = new Constraint(parentTaskId, dataSize);
						successorConstraintList.add(tempParentConstraint);
					}
					
					//创建新任务
					WTask wTask = new WTask(taskId, workflowId, baseExecutionTime);
					wTask.getParentIDList().addAll(parentConstraintList);
					wTask.getSuccessorIDList().addAll(successorConstraintList);
					
					taskList.add(wTask);
				}//end for(TempWTask task: tempWTaskList) //读取任务信息结束				
								
				String name = readWorkflow.getWorkflowName();
				Workflow workflow = new Workflow(-1, name, -1, -1, -1);
				workflow.setTaskList(taskList); //将任务加入到工作流中
				
				templateWList.add(workflow);
			}			
			si.close(); //读取样本工作流结束
		}catch(IOException e){System.out.println(e.getMessage());}
		
		if(templateWList.size() != 15)
		{
			System.out.println("Error: the template workflow is not right");
		}
		
		List<Workflow> deleteWorkflows = new ArrayList<Workflow>();
		for(Workflow workflow: templateWList)
		{//将Epigenomics类的工作流去掉
			if(workflow.getWorkflowName().equals("Epigenomics_24.xml")||
					workflow.getWorkflowName().equals("Epigenomics_46.xml")||
					workflow.getWorkflowName().equals("Epigenomics_100.xml"))
			{
				deleteWorkflows.add(workflow);
			}
		}
		templateWList.removeAll(deleteWorkflows);
				
		//计算每个模板工作流的makespan
		for(Workflow workflowMakespan: templateWList)
		{
			double makespan = CalculateMakespan(workflowMakespan);									
			workflowMakespan.setMakespan(makespan);
			
//			System.out.println("Makespan: "+makespan+" Workflow Name: "+workflowMakespan.getWorkflowName());
		}
		
		//开始产生测试集
		List<Workflow> wList = new ArrayList<Workflow>();
		int workflowId = 0;
		int arrivalTime = 0;		
		while(wList.size() < workflowNum) //workflowNum表示测试集中任务的数量
		{
			int temNum = PoissValue(StaticfinalTags.arrivalLamda); //某时刻到达的工作流数量
			if(temNum == 0) //如果到达工作流数量为0，则转到下一时刻
			{
				arrivalTime++; //到下一个时刻
				continue;
			}
			else
			{
				boolean flag = false; //判断工作流数量是否已经达到指定数量
				for(int i=0; i<temNum; i++)
				{
//					System.out.println("Workflow Num: "+wList.size());
//					int templateNum = StaticfinalTags.selectedNum;//(int)(Math.random()*12);//确定模板中的哪个工作流
//					selectedNum++;
					int templateNum = (int)(Math.random()*12);//确定模板中的哪个工作流
					
					Workflow findWorkflow = templateWList.get(templateNum);
					
					//找出该工作流模板的参数
					List<WTask> tempTaskList = new ArrayList<WTask>();//把工作流的任务复制出来										
					for(WTask task: findWorkflow.getTaskList())
					{
/*						if(task.getBaseExecutionTime() == 0)
						{
							System.out.println("Execution Time: "+task.getBaseExecutionTime());
						}*/
						WTask copyTask = new WTask(task.getTaskId(), workflowId, task.getBaseExecutionTime());
						copyTask.setBaseStartTime(task.getBaseStartTime());
						copyTask.setBaseFinishTime(task.getBaseFinishTime());
						
						//将父任务ID进行关联
						List<Constraint> parentConstraintList = new ArrayList<Constraint>();
						for(Constraint con: task.getParentIDList())
						{
							String parentTaskId = con.getTaskId();
							int dataSize = con.getDataSize();
							Constraint tempParentConstraint = new Constraint(parentTaskId, dataSize);
							parentConstraintList.add(tempParentConstraint);
						}
						
						//将子任务ID进行关联
						List<Constraint> successorConstraintList = new ArrayList<Constraint>();
						for(Constraint con: task.getSuccessorIDList())
						{
							String parentTaskId = con.getTaskId();
							int dataSize = con.getDataSize();
							Constraint tempSuccessorConstraint = new Constraint(parentTaskId, dataSize);
							successorConstraintList.add(tempSuccessorConstraint);
						}
																		
						copyTask.getParentIDList().addAll(parentConstraintList);
						copyTask.getSuccessorIDList().addAll(successorConstraintList);
						
						tempTaskList.add(copyTask);
					}
					
					//将任务进行关联
					for(WTask connectedTask: tempTaskList)//遍历每个任务
					{
						for(Constraint parentCon: connectedTask.getParentIDList())
						{//关联任务connectedTask的所有父任务
							String parentID = parentCon.getTaskId();
							int dataSize = parentCon.getDataSize();
							for(WTask parentTask: tempTaskList)
							{
								if(parentID.equals(parentTask.getTaskId()))
								{//找出任务connectedTask的父任务
									ConstraintWTask parent = new ConstraintWTask(parentTask, dataSize);
									connectedTask.getParentTaskList().add(parent);
									break;
								}
							}
						}//关联任务connectedTask父任务结束
						
						for(Constraint successorCon: connectedTask.getSuccessorIDList())
						{//关联任务connectedTask的所有子任务
							String successorID = successorCon.getTaskId();
							int dataSize = successorCon.getDataSize();
							for(WTask successorTask: tempTaskList)
							{
								if(successorID.equals(successorTask.getTaskId()))
								{//找出任务connectedTask的子任务
									ConstraintWTask successor = new ConstraintWTask(successorTask, dataSize);
									connectedTask.getSuccessorTaskList().add(successor);								
									break;
								}
							}
						}//关联任务connectedTask子任务结束
					}//关联任务结束
					
					String name = findWorkflow.getWorkflowName();					
					int deadline = (int)(arrivalTime + findWorkflow.getMakespan()*StaticfinalTags.deadlineBase);
					
					Workflow newWorkflow = new Workflow(workflowId, name, arrivalTime, findWorkflow.getMakespan(), deadline);
					newWorkflow.setTaskList(tempTaskList);
					
					wList.add(newWorkflow);
					
					if(wList.size() == workflowNum)
					{
						flag = true;
						break;
					}					
					workflowId++; //递增工作流的Id
				}				
				if(flag)
				{
					break; 
				}				
				arrivalTime++; //到下一个时刻
			}						
		}//end while(wList.size() < workflowNum)
		return wList;
	}//产生工作流测试集结束
	
	/**计算工作流的makespan*/
	public static double CalculateMakespan(Workflow cWorkflow)
	{
		List<WTask> calculatedTaskList = new ArrayList<WTask>();	
		while(true)
		{
			for(WTask task: cWorkflow.getTaskList())//对所有任务进行遍历一次
			{
				if(task.getBaseFinishTime() > -1) //如果任务已经计算过，则跳过
				{
					continue;
				}
				
				double executionTime = task.getBaseExecutionTime(); //任务的执行时间 int double	
				if(task.getParentIDList().size() == 0)
				{//开始任务
					task.setBaseStartTime(0);
					task.setBaseFinishTime(executionTime);
					calculatedTaskList.add(task);
				}
				else //非开始任务,也就是有父任务的任务
				{	
					double maxStartTime = -1; //最晚开始时间
					boolean unCalculatedParent = false; //是否存在未计算的父任务
					
					double maxDataSize = 0; //最大的数据量
					for(Constraint con: task.getParentIDList())
					{
						if(con.getDataSize() > maxDataSize)
						{
							maxDataSize = con.getDataSize();
						}
					}
					int round = (task.getParentIDList().size()/StaticfinalTags.portNum + 2); //可能并发的数量
					double commDelay = (round*maxDataSize)/StaticfinalTags.bandwidth;
					
					for(Constraint con: task.getParentIDList())
					{//判断是否每个父任务都计算过，如果是，那么找出任务task的最早开始时间
						unCalculatedParent = true;						
						String parentId = con.getTaskId(); //父任务的Id
												
						for(WTask parTask: calculatedTaskList)
						{
							if(parentId.equals(parTask.getTaskId()))
							{
								unCalculatedParent = false;
								double startTime = parTask.getBaseFinishTime() + commDelay;
								if(startTime > maxStartTime)
								{
									maxStartTime = startTime;
								}
								break;
							}
						}
						if(unCalculatedParent == true)
						{
							break;
						}					
					}
					
					if(unCalculatedParent == false)
					{//如果该任务所有父任务都完成了，那么计算它的开始和结束时间
						task.setBaseStartTime(maxStartTime);
						task.setBaseFinishTime(maxStartTime + executionTime);
						calculatedTaskList.add(task);
					}																															
				}//end else
			}//end for(Task task: cWorkflow.getTaskList()) 
			
			if(calculatedTaskList.size() == cWorkflow.getTaskList().size())//计算完的条件
			{
				break;
			}
		}//end while
		
		double makespan = 0; //任务的最大完成时间就是makespan int double
		for(WTask cTask: cWorkflow.getTaskList())
		{
			if(cTask.getBaseFinishTime() > makespan)
			{
				makespan = cTask.getBaseFinishTime();
			}
			if(cTask.getBaseFinishTime() < 0)
			{
				throw new IllegalArgumentException("Error: there exists tasks is not calculated in calculate makespan!");
//				System.out.println("Error: there exists tasks is not calculated in calculate makespan");
			}
		}
		if(makespan<=0)
		{//???可以去掉
			throw new IllegalArgumentException("Error: occur error in calculate makespan!");
//			System.out.println("Error: occur error in calculate makespan");
		}
		return makespan;
	}//结束工作流makespan的计算
	
	/**设置任务真正的基准执行时间*/
	public static void calculateRealTaskBaseExecutionTime(List<Workflow> list)
	{
		for(Workflow tempWorkflow: list)
		{
			for(WTask tempTask: tempWorkflow.getTaskList())
			{
				double standardDeviation = tempTask.getBaseExecutionTime()*StaticfinalTags.standardDeviation;
				//生成任务的真正运行时间
				double realBaseExecutionTime = NormalDistributionCos(tempTask.getBaseExecutionTime(), standardDeviation);
				
				//????在这里增加其他概率分布的生成器
				
				if(realBaseExecutionTime < 0)
				{
					realBaseExecutionTime = -realBaseExecutionTime;
				}				
				if(realBaseExecutionTime == 0)
				{
					throw new IllegalArgumentException("The execution time of a task is zero!");
//					realBaseExecutionTime = 1;
				}
				
//				int executionTimeWithConfidency = (int)(tempTask.getBaseExecutionTime()+Math.sqrt(standardDeviation)*getQuantile(StaticfinalTags.confidency));
				tempTask.setRealBaseExecutionTime(realBaseExecutionTime);
//				tempTask.setExecutionTimeWithConfidency(executionTimeWithConfidency);
			}
		}
	}
	
	//根据到达时间升序排序任务，需要导入包java.util.Comparator
	public static class WorkflowComparatorById implements Comparator<Workflow>
	{
		public int compare(Workflow w1, Workflow w2)
		{
			return (w1.getWorkflowId() - w2.getWorkflowId());
		}
	}
	
	/**泊松分布数值的产生*/
	public static int PoissValue(double Lamda)
	{//产生的数值value是符合泊松分布的，均值和方差都是Lamda
		 int value=0;
		 double b=1;
		 double c=0;
		 c=Math.exp(-Lamda); 
		 double u=0;
		 do 
		 {
			 u=Math.random();
			 b*=u;
			 if(b>=c)
				 value++;
		  }while(b>=c);
		 return value;
	}
	
	/**
	 * 正态分布数值生成器
	 * @param average 均值
	 * @param deviance 方差
	 * @return 
	 */
	public static double NormalDistributionCos(double average,double deviance)
	{
		double Pi=3.1415926535;
		double r1=Math.random();
		Math.random();Math.random();Math.random();Math.random();Math.random();
		Math.random();Math.random();
		double r2=Math.random();
		double u=Math.sqrt((-2)*Math.log(r1))*Math.cos(2*Pi*r2);
		double z=average+u*Math.sqrt(deviance);
		return z;
	}	
}
