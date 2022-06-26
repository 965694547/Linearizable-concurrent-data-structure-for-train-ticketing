package ticketingsystem;

import java.util.Random;
import java.util.Vector;
import java.util.concurrent.locks.*;

public class TicketingDS implements TicketingSystem {
	//ToDo
	private long tid; //车票编号
	private int routenum; //列车车次数目
	private int coachnum; //车厢数目
	private int seatnum; //座位数目
	private int stationnum; //站数目
	private int threadnum; //线程数目

	private int seatsum; //单趟列车总座位数目，因为每节车厢的座位数目没有意义
	private boolean [][][]seatmap; //座位图 车次*座位*站台
	private ReentrantReadWriteLock []lock; //读写锁：允许多个读线程同时访问，但不允许写线程和读线程、写线程和写线程同时访问。

	public TicketingDS(){ //无参数初始化为缺省值
		tid=0L;
		routenum=5;
		coachnum=8;
		seatnum=100;
		stationnum=10;
		threadnum=16;
		seatsum=coachnum*seatnum;
		lock=new ReentrantReadWriteLock[routenum];
		for(int i=0;i<routenum;i++){
			lock[i]=new ReentrantReadWriteLock();
		}
		seatmap=new boolean[routenum][seatsum][stationnum];
	}

	public TicketingDS(int routenum, int coachnum, int seatnum, int stationnum, int threadnum) throws InterruptedException{ 
		tid=0L;
		this.routenum=routenum;
		this.coachnum=coachnum;
		this.seatnum=seatnum;
		this.stationnum=stationnum;
		this.threadnum=threadnum;
		seatsum=coachnum*seatnum;
		lock=new ReentrantReadWriteLock[routenum];
		for(int i=0;i<routenum;i++){
			lock[i]=new ReentrantReadWriteLock(true);
		}
		seatmap=new boolean[routenum][seatsum][stationnum];
	}

	//退票方法满足可线性化要求 
	//对有效的Ticket对象返回true，对错误或无效的Ticket对象返回false
	//退票后，原车票的tid作废
	@Override
	public boolean refundTicket(Ticket ticket) {
		Lock wlock=lock[ticket.route-1].writeLock();
		wlock.lock();
		try{
			int seat=(ticket.coach-1)*seatnum + ticket.seat-1;
			for(int j=ticket.departure;j<ticket.arrival;j++){
				seatmap[ticket.route-1][seat][j-1]=false;
			}
			return true;
		}finally{
			wlock.unlock();
		}
	}

	//买票方法满足可线性化要求
	//若购票成功，返回有效的Ticket对象；若失败（即无余票），返回无效的Ticket对象（即return null）
	//离开站已经确定大于出发站
	//每个区段有余票时，系统必须满足该区段的购票请求。
	//车票不能超卖，系统不能卖无座车票。
	//每一个tid的车票只能出售一次。
	@Override
	public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
		Lock wlock=lock[route-1].writeLock();
		wlock.lock();
		try{
			int i,j;
			for(i=0;i<seatsum;i++){
				for(j=departure;j<arrival;j++){
					if (seatmap[route-1][i][j-1]==true){
						break;
					}
				}
				if (j==arrival){
					break;
				}
			}
			if(i==seatsum){
				return null;
			}
			for(j=departure;j<arrival;j++){
				seatmap[route-1][i][j-1]=true;
			}
			Ticket ticket = new Ticket();
			ticket.tid=tid++;
			ticket.passenger=passenger;
			ticket.route=route;
			ticket.coach=i/seatnum+1;
			ticket.seat=i%seatnum+1;
			ticket.departure=departure;
			ticket.arrival=arrival;
			return ticket;
		}finally{
			wlock.unlock();
		}
	}

	//查询余票方法满足静态一致性
	//查询route车次从departure站到arrival站的余票数
	//离开站已经确定大于出发站
	@Override
	public int inquiry(int route, int departure, int arrival){
		int count=0;
		Lock rlock=lock[route-1].readLock();
		rlock.lock();
		try{
			int i,j;
			for(i=0;i<seatsum;i++){
				for(j=departure;j<arrival;j++){
					if(seatmap[route-1][i][j-1]==true){
						break;
					}
				}
				if(j==arrival){
					count++;
				}
			}
			return count;
		}finally{
			rlock.unlock();
		}
	}
}