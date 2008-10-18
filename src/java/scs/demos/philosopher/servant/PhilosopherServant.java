package scs.demos.philosopher.servant;

import java.util.Random;

import scs.core.ConnectionDescription;
import scs.core.IReceptacles;
import scs.core.IReceptaclesHelper;
import scs.core.InvalidName;
import scs.demos.philosopher.Fork;
import scs.demos.philosopher.ForkHelper;
import scs.demos.philosopher.Observer;
import scs.demos.philosopher.ObserverHelper;
import scs.demos.philosopher.PhilosopherPOA;
import scs.demos.philosopher.PhilosopherState;
import scs.demos.philosopher.StatusInfo;

public class PhilosopherServant extends PhilosopherPOA {

	protected String name = null;
	protected PhilosopherComponent philosopher = null;
	protected PhilosopherThread phiThread;
	protected IReceptacles leftHand = null;
	protected IReceptacles rightHand = null;
	protected IReceptacles infoReceptacle = null;
	protected PhilosopherState state = PhilosopherState.THINKING;
	protected StatusInfo statusInfo = new StatusInfo();
	protected int hunger = 0;
	protected boolean hasRightFork = false;
	protected boolean hasLeftFork = false;

	public PhilosopherServant(PhilosopherComponent philosopher)
	{
		this.philosopher = philosopher;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	/**
	 *
	 */
	private class PhilosopherThread extends Thread {
		private boolean stopped = false;
		
		PhilosopherServant phi = null;
		
		@Override
		public void run() {
			while( ! this.stopped ){
				phi.update();
				phi.notifyObservers();
				phi.sleep();
			}	
		}
		
		public PhilosopherThread(PhilosopherServant servant)
		{
			super(servant.name);
			phi = servant;
		}
	}

	public boolean isHungry() {
		return 	this.hunger > 3;
	}

	private void eatSome() {
		this.hunger = this.hunger - 3;
		if ( ! isHungry() )
		{
			this.hunger = 0;
		}
	}
	
	private void releaseForks() {
		//System.out.println(name + " droped forks.");
		this.releaseLeftFork();
		this.releaseRightFork();
		this.notifyObservers();
	}
	
	private void getMoreHungry() {
		this.hunger++;
	}

	private void avoidDeadlock() {
		
		Random r = new Random(System.currentTimeMillis());
		
		if( ( (this.hasLeftFork && ! this.hasRightFork ) 
				|| 	(!this.hasLeftFork && this.hasRightFork ) )
				&& r.nextBoolean() ) {
			
			if( this.hasLeftFork  ) {
				this.releaseLeftFork();
			}
			else if( this.hasRightFork ) {
				this.releaseRightFork();
			}

			this.notifyObservers();
		}
	}

	private void update() {
		
		if( this.hasLeftFork && this.hasRightFork ) {
			if( this.isHungry() ) {
				this.setState(PhilosopherState.EATING);
				this.eatSome();
			} else { 
				this.releaseForks();
				this.setState(PhilosopherState.THINKING);
			}
			
		}  else {

			this.getMoreHungry();
			
			if( this.hunger < 3 )
				setState( PhilosopherState.THINKING );
			else if( this.hunger < 10 )
				setState(PhilosopherState.HUNGRY);
			else if( this.hunger < 40 )
				setState(PhilosopherState.STARVING);
			else 
				setState(PhilosopherState.DEAD);
			
			if( this.isHungry() ) {
				if( this.tryGetFork("left") ) 
					return;
				if( this.tryGetFork("right") ) 
					return;

				this.avoidDeadlock();
			}
		}
/*
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		*/
	}
	
	void sleep() {
		Random r = new Random();
		
		try {
			Thread.sleep( 500 + Math.round( 2000 * r.nextFloat() ) );
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
	}
	
	boolean getRightFork()
	{
		if( this.hasRightFork )
			return true;
		
		Fork fork = null;
		try {
			fork = ForkHelper.narrow(rightHand.getConnections("right")[0].objref);
			this.hasRightFork = fork.get();
		} catch (InvalidName e) {
			e.printStackTrace();
		}
		return this.hasRightFork;
	}
	
	boolean getLeftFork()
	{
		if( this.hasLeftFork )
			return true;
		
		Fork fork = null;
		try {
			fork = ForkHelper.narrow( leftHand.getConnections("left")[0].objref );
			this.hasLeftFork = fork.get();
		} catch (InvalidName e) {
			e.printStackTrace();
		}

		return this.hasLeftFork;
	}
				
	boolean releaseLeftFork()
	{
		Fork fork = null;
		try {
			fork = ForkHelper.narrow( leftHand.getConnections("left")[0].objref );
		} catch (InvalidName e) {
			e.printStackTrace();
		}
		
		this.hasLeftFork = false;
		return fork.release();
	}
	
	boolean releaseRightFork()
	{
		Fork fork = null;
		
		try {
			fork = ForkHelper.narrow(rightHand.getConnections("right")[0].objref);
		} catch (InvalidName e) {
			e.printStackTrace();
		}

		this.hasRightFork = false;
		return fork.release();
	}
	
	public boolean start() {

		rightHand = IReceptaclesHelper.narrow(philosopher.getFacetByName("right"));
		leftHand = IReceptaclesHelper.narrow(philosopher.getFacetByName("left"));
		infoReceptacle = IReceptaclesHelper.narrow(philosopher.getFacetByName("info"));

		phiThread = new PhilosopherThread(this);
		phiThread.start();
		return true;
	}

	private boolean tryGetFork(String fork) {
		
		boolean hasFork = fork.equals("left") ? this.hasLeftFork : this.hasRightFork;
		
		if( !hasFork ) {
			if( (fork.equals("left") && this.getLeftFork())
				|| (fork.equals("right") && this.getRightFork()) ) {
				
				//System.err.println( name + " got " + fork + " fork." );
				this.notifyObservers();
				return true;
			}
		}
		return false;
	}

	private void setState( PhilosopherState st ) {
		this.state = st;
		//this.notifyObservers();
	}
	
	private void notifyObservers() {

		statusInfo.name = this.name;
		statusInfo.state = this.state;
		statusInfo.has_left_fork = this.hasLeftFork;
		statusInfo.has_right_fork = this.hasRightFork;
		statusInfo.ticks_since_last_meal = this.hunger;
		
		ConnectionDescription conns[];
		try {
		
			conns = infoReceptacle.getConnections("info");
			for (int i = 0; i < conns.length; i++) {
				Observer obs = ObserverHelper.narrow( conns[i].objref );
				obs.push(statusInfo);
			}

		} catch (InvalidName e) {
			e.printStackTrace();
		}
	}
}


