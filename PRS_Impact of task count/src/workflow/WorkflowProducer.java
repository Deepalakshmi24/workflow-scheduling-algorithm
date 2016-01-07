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
	private static List<Workflow> workflowList; //����������
	
	public static void main(String[] args) throws ClassNotFoundException, IOException 
	{
		workflowList = new ArrayList<Workflow>(); //��ʼ���������б�
		int workflowNum = StaticfinalTags.workflowNum; //��һ��ɨβ�Ĺ�����
		workflowList = produceWorkflow(workflowNum); //�������������Լ�		
//		System.out.println("Num: "+workflowList.size());
		
		calculateRealTaskBaseExecutionTime(workflowList); //��ֵ�����й�������ÿ��������������
		
		//��������д���ļ�tempWorkflow.txt��,�Թ��Ա�����ʹ��
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
	
	/**���ݹ�����ģ�壬�������������Լ�*/
	private static List<Workflow> produceWorkflow(int workflowNum) throws IOException, ClassNotFoundException
	{	
//		System.out.println("Start produce workflow!");
		//��ȡ����������
		List<Workflow> templateWList = new ArrayList<Workflow>();
		FileInputStream fi = new FileInputStream("tempWorkflow.txt");
		ObjectInputStream si = new ObjectInputStream(fi);
		try
		{
			for(int i=0; i<15; i++)
			{
				TempWorkflow readWorkflow = (TempWorkflow)si.readObject();
				
				List<WTask> taskList = new ArrayList<WTask>(); //�����������񼯺�
				List<TempWTask> tempWTaskList = readWorkflow.getTaskList();//ģ�幤�����е����񼯺�	
				for(TempWTask task: tempWTaskList)
				{//���ƹ�����readWorkflow�е�ÿ������
					String taskId = task.getTaskId(); //�����ID
					int workflowId = -1; //��ʼ���������ڹ�������ID
					double baseExecutionTime =  task.getTaskRunTime(); //����Ļ�׼ִ��ʱ��
					if(baseExecutionTime < 0)
					{
						throw new IllegalArgumentException("Task base execution time is less than zero!");
//						baseExecutionTime = 1;
//						System.out.println("task exeuction time is less than one second");
					}
					
					//���ø����񼯺�
					List<Constraint> parentConstraintList = new ArrayList<Constraint>();
					for(Constraint con: task.getParentTaskList())
					{
						String parentTaskId = con.getTaskId();
						int dataSize = con.getDataSize();
						Constraint tempParentConstraint = new Constraint(parentTaskId, dataSize);
						parentConstraintList.add(tempParentConstraint);
					}
					
					//�����񼯺�
					List<Constraint> successorConstraintList = new ArrayList<Constraint>();
					for(Constraint con: task.getSuccessorTaskList())
					{
						String parentTaskId = con.getTaskId();
						int dataSize = con.getDataSize();
						Constraint tempParentConstraint = new Constraint(parentTaskId, dataSize);
						successorConstraintList.add(tempParentConstraint);
					}
					
					//����������
					WTask wTask = new WTask(taskId, workflowId, baseExecutionTime);
					wTask.getParentIDList().addAll(parentConstraintList);
					wTask.getSuccessorIDList().addAll(successorConstraintList);
					
					taskList.add(wTask);
				}//end for(TempWTask task: tempWTaskList) //��ȡ������Ϣ����				
								
				String name = readWorkflow.getWorkflowName();
				Workflow workflow = new Workflow(-1, name, -1, -1, -1);
				workflow.setTaskList(taskList); //��������뵽��������
				
				templateWList.add(workflow);
			}			
			si.close(); //��ȡ��������������
		}catch(IOException e){System.out.println(e.getMessage());}
		
		if(templateWList.size() != 15)
		{
			System.out.println("Error: the template workflow is not right");
		}
		
		List<Workflow> deleteWorkflows = new ArrayList<Workflow>();
		for(Workflow workflow: templateWList)
		{//��Epigenomics��Ĺ�����ȥ��
			if(workflow.getWorkflowName().equals("Epigenomics_24.xml")||
					workflow.getWorkflowName().equals("Epigenomics_46.xml")||
					workflow.getWorkflowName().equals("Epigenomics_100.xml"))
			{
				deleteWorkflows.add(workflow);
			}
		}
		templateWList.removeAll(deleteWorkflows);
				
		//����ÿ��ģ�幤������makespan
		for(Workflow workflowMakespan: templateWList)
		{
			double makespan = CalculateMakespan(workflowMakespan);									
			workflowMakespan.setMakespan(makespan);
			
//			System.out.println("Makespan: "+makespan+" Workflow Name: "+workflowMakespan.getWorkflowName());
		}
		
		//��ʼ�������Լ�
		List<Workflow> wList = new ArrayList<Workflow>();
		int workflowId = 0;
		int arrivalTime = 0;		
		while(wList.size() < workflowNum) //workflowNum��ʾ���Լ������������
		{
			int temNum = PoissValue(StaticfinalTags.arrivalLamda); //ĳʱ�̵���Ĺ���������
			if(temNum == 0) //������﹤��������Ϊ0����ת����һʱ��
			{
				arrivalTime++; //����һ��ʱ��
				continue;
			}
			else
			{
				boolean flag = false; //�жϹ����������Ƿ��Ѿ��ﵽָ������
				for(int i=0; i<temNum; i++)
				{
//					System.out.println("Workflow Num: "+wList.size());
//					int templateNum = StaticfinalTags.selectedNum;//(int)(Math.random()*12);//ȷ��ģ���е��ĸ�������
//					selectedNum++;
					int templateNum = (int)(Math.random()*12);//ȷ��ģ���е��ĸ�������
					
					Workflow findWorkflow = templateWList.get(templateNum);
					
					//�ҳ��ù�����ģ��Ĳ���
					List<WTask> tempTaskList = new ArrayList<WTask>();//�ѹ������������Ƴ���										
					for(WTask task: findWorkflow.getTaskList())
					{
/*						if(task.getBaseExecutionTime() == 0)
						{
							System.out.println("Execution Time: "+task.getBaseExecutionTime());
						}*/
						WTask copyTask = new WTask(task.getTaskId(), workflowId, task.getBaseExecutionTime());
						copyTask.setBaseStartTime(task.getBaseStartTime());
						copyTask.setBaseFinishTime(task.getBaseFinishTime());
						
						//��������ID���й���
						List<Constraint> parentConstraintList = new ArrayList<Constraint>();
						for(Constraint con: task.getParentIDList())
						{
							String parentTaskId = con.getTaskId();
							int dataSize = con.getDataSize();
							Constraint tempParentConstraint = new Constraint(parentTaskId, dataSize);
							parentConstraintList.add(tempParentConstraint);
						}
						
						//��������ID���й���
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
					
					//��������й���
					for(WTask connectedTask: tempTaskList)//����ÿ������
					{
						for(Constraint parentCon: connectedTask.getParentIDList())
						{//��������connectedTask�����и�����
							String parentID = parentCon.getTaskId();
							int dataSize = parentCon.getDataSize();
							for(WTask parentTask: tempTaskList)
							{
								if(parentID.equals(parentTask.getTaskId()))
								{//�ҳ�����connectedTask�ĸ�����
									ConstraintWTask parent = new ConstraintWTask(parentTask, dataSize);
									connectedTask.getParentTaskList().add(parent);
									break;
								}
							}
						}//��������connectedTask���������
						
						for(Constraint successorCon: connectedTask.getSuccessorIDList())
						{//��������connectedTask������������
							String successorID = successorCon.getTaskId();
							int dataSize = successorCon.getDataSize();
							for(WTask successorTask: tempTaskList)
							{
								if(successorID.equals(successorTask.getTaskId()))
								{//�ҳ�����connectedTask��������
									ConstraintWTask successor = new ConstraintWTask(successorTask, dataSize);
									connectedTask.getSuccessorTaskList().add(successor);								
									break;
								}
							}
						}//��������connectedTask���������
					}//�����������
					
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
					workflowId++; //������������Id
				}				
				if(flag)
				{
					break; 
				}				
				arrivalTime++; //����һ��ʱ��
			}						
		}//end while(wList.size() < workflowNum)
		return wList;
	}//�������������Լ�����
	
	/**���㹤������makespan*/
	public static double CalculateMakespan(Workflow cWorkflow)
	{
		List<WTask> calculatedTaskList = new ArrayList<WTask>();	
		while(true)
		{
			for(WTask task: cWorkflow.getTaskList())//������������б���һ��
			{
				if(task.getBaseFinishTime() > -1) //��������Ѿ��������������
				{
					continue;
				}
				
				double executionTime = task.getBaseExecutionTime(); //�����ִ��ʱ�� int double	
				if(task.getParentIDList().size() == 0)
				{//��ʼ����
					task.setBaseStartTime(0);
					task.setBaseFinishTime(executionTime);
					calculatedTaskList.add(task);
				}
				else //�ǿ�ʼ����,Ҳ�����и����������
				{	
					double maxStartTime = -1; //����ʼʱ��
					boolean unCalculatedParent = false; //�Ƿ����δ����ĸ�����
					
					double maxDataSize = 0; //����������
					for(Constraint con: task.getParentIDList())
					{
						if(con.getDataSize() > maxDataSize)
						{
							maxDataSize = con.getDataSize();
						}
					}
					int round = (task.getParentIDList().size()/StaticfinalTags.portNum + 2); //���ܲ���������
					double commDelay = (round*maxDataSize)/StaticfinalTags.bandwidth;
					
					for(Constraint con: task.getParentIDList())
					{//�ж��Ƿ�ÿ�������񶼼����������ǣ���ô�ҳ�����task�����翪ʼʱ��
						unCalculatedParent = true;						
						String parentId = con.getTaskId(); //�������Id
												
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
					{//������������и���������ˣ���ô�������Ŀ�ʼ�ͽ���ʱ��
						task.setBaseStartTime(maxStartTime);
						task.setBaseFinishTime(maxStartTime + executionTime);
						calculatedTaskList.add(task);
					}																															
				}//end else
			}//end for(Task task: cWorkflow.getTaskList()) 
			
			if(calculatedTaskList.size() == cWorkflow.getTaskList().size())//�����������
			{
				break;
			}
		}//end while
		
		double makespan = 0; //�����������ʱ�����makespan int double
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
		{//???����ȥ��
			throw new IllegalArgumentException("Error: occur error in calculate makespan!");
//			System.out.println("Error: occur error in calculate makespan");
		}
		return makespan;
	}//����������makespan�ļ���
	
	/**�������������Ļ�׼ִ��ʱ��*/
	public static void calculateRealTaskBaseExecutionTime(List<Workflow> list)
	{
		for(Workflow tempWorkflow: list)
		{
			for(WTask tempTask: tempWorkflow.getTaskList())
			{
				double standardDeviation = tempTask.getBaseExecutionTime()*StaticfinalTags.standardDeviation;
				//�����������������ʱ��
				double realBaseExecutionTime = NormalDistributionCos(tempTask.getBaseExecutionTime(), standardDeviation);
				
				//????�����������������ʷֲ���������
				
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
	
	//���ݵ���ʱ����������������Ҫ�����java.util.Comparator
	public static class WorkflowComparatorById implements Comparator<Workflow>
	{
		public int compare(Workflow w1, Workflow w2)
		{
			return (w1.getWorkflowId() - w2.getWorkflowId());
		}
	}
	
	/**���ɷֲ���ֵ�Ĳ���*/
	public static int PoissValue(double Lamda)
	{//��������ֵvalue�Ƿ��ϲ��ɷֲ��ģ���ֵ�ͷ����Lamda
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
	 * ��̬�ֲ���ֵ������
	 * @param average ��ֵ
	 * @param deviance ����
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
