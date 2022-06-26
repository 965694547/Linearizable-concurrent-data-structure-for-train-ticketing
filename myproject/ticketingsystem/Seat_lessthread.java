package ticketingsystem;

import ticketingsystem.Seat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Seat_lessthread implements Seat{
    private int seatsum; //单趟列车总座位数目，因为每节车厢的座位数目没有意义

    private AtomicLong[] seatmap;//座位图，用于存储座位在每一站被占用情况
    private ConcurrentHashMap<Long, AtomicInteger> priorefund;//退票购票记录，key为出发站和终止站，value为最后一步操作的位置，该位置可以是购票的位置，但必须是退票的位置，主要是为了用于查询
    private AtomicInteger capicity; //当前整个车次承载量，LongAdder在较多线程可能会出错且速度较慢
    private int cap; //当前整个车次承载量，LongAdder在较多线程可能会出错且速度较慢

    public Seat_lessthread(int seatsum){
		this.seatsum=seatsum;

        seatmap=new AtomicLong[seatsum];
        for (int i=0; i<seatsum; i++) {
            seatmap[i] = new AtomicLong();
        }
        priorefund = new ConcurrentHashMap<Long, AtomicInteger>();//所有被购买过的票，无论是否真的被买过
        capicity = new AtomicInteger();
        cap = capicity.get();
    }

    private long encoder(int departure, int arrival){//输出当前出发到截至的位置编码
        long coder = 0;
        for (int j = departure - 1; j < arrival - 1; j++){
            coder += 1 << j;
        }
        return coder;
    }

    public int buyTicket(int departure, int arrival){//购买车票
        long coder = encoder(departure,arrival);
        if (!priorefund.containsKey(coder)) { //如果没有买过该票，创建该票
            priorefund.put(coder, new AtomicInteger());
        }
        long newseat, oldseat;
        int end = seatsum;
        if(cap<seatsum){
            end =cap;
            for (int i = cap; i < seatsum; i++){
                AtomicLong currmap=seatmap[i];
                oldseat=currmap.get();//座位占用情况
                while ((oldseat & coder) == 0){//座位未被占用
                    newseat = oldseat | coder;
                    if (currmap.compareAndSet(oldseat, newseat)){//执行成功说明购票成功
                        capicity.getAndIncrement();
                        return i;
                    }
                    oldseat=currmap.get();
                }
            }
            cap=capicity.get();
        }
        AtomicInteger currpriorefund = priorefund.get(coder);//退回车票
        int currnum = currpriorefund.get();
        for (int i = currnum; i <end; i++){
            AtomicLong currmap=seatmap[i];
            oldseat=currmap.get();//座位占用情况
            while ((oldseat & coder) == 0){//座位未被占用
                newseat = oldseat | coder;
                if (currmap.compareAndSet(oldseat, newseat)){//执行成功说明购票成功
                    currpriorefund.compareAndSet(currnum,i+1);//当前位置被购买只能期待下一个位置没有被购买
                    return i;
                }
                oldseat=currmap.get();
            }
        }
        for (int i = 0; i <currnum; i++){
            AtomicLong currmap=seatmap[i];
            oldseat=currmap.get();//座位占用情况
            while ((oldseat & coder) == 0){//座位未被占用
                newseat = oldseat | coder;
                if (currmap.compareAndSet(oldseat, newseat)){//执行成功说明购票成功
                    currpriorefund.compareAndSet(currnum,i+1);//当前位置被购买只能期待下一个位置没有被购买
                    return i;
                }
                oldseat=currmap.get();
            }
        }
        return -1;
    }

    public boolean refundTicket(Ticket ticket, int i){//查询车票
        AtomicLong currmap = seatmap[i];
        long oldseat = currmap.get();//旧座位是否被占
        long newseat;
        long coder = encoder(ticket.departure, ticket.arrival);
        AtomicInteger currpriorefund = priorefund.get(coder);
        while ((oldseat & coder) == coder){//旧座位没有被退才会执行
            newseat = (~coder) &oldseat;
            if (currmap.compareAndSet(oldseat,newseat)) {//执行成功说明退票成功
                int currnum = currpriorefund.get();
                while (i<currnum) {//执行循环说明该票没有从中剔除
                    currpriorefund.compareAndSet(currnum, i);//i已经隐含减1
                    currnum = currpriorefund.get();
                }
                return true;
            }
            oldseat = currmap.get();
        }
        return false;
    }

    public int inquery(int departure, int arrival){//返回剩余车票
        int num = 0;//余票数
        int end = seatsum;
        long coder = encoder(departure, arrival);
        if(cap<seatsum){
            cap = capicity.get();//获取当前座位数目
            end=cap<seatsum?cap:seatsum;
            num+=seatsum-end;
        }
        for (int i = 0; i < end; i++) {
            AtomicLong currcoder = seatmap[i];
            if ((currcoder.get() & coder) == 0)//已经卖出的票与当前票完全不重合票数才会+1
                num++;
        }
        return num;
    }
}