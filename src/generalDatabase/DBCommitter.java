package generalDatabase;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;

import javax.swing.Timer;

import PamUtils.PamCalendar;
import warnings.PamWarning;
import warnings.WarningSystem;

/**
 * Class to handle regular commits of the data when Auto Commit is not
 * set. Data commits will occur at least every n seconds and at least
 * every m data writes, but not committing every record should speed things up 
 * a little.  
 * @author dg50
 *
 */
public class DBCommitter {

	private DBControl dbControl;
	
	private int unComitted = 0;

	/**
	 * Rows written between commits. Each SQLite commit waits for the disk, and commitNow is
	 * synchronized, so every module writing to the database waits in turn. At 10 rows, a
	 * run writing a row per Deep Learning segment spent more time committing than classifying.
	 * The timer below still commits any remainder within 3 s, and Stop commits everything.
	 */
	private static final int COMMIT_ROWS = 500;
	
	private long lastCommitTime = 0;

	private Timer swingTimer;

	private PamWarning commitWarning;
	
//	private 

	public DBCommitter(DBControl dbControl) {
		super();
		this.dbControl = dbControl;

		commitWarning = new PamWarning("Database System", null, 2);
		
		swingTimer = new Timer(3000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				commitTimer();
			}
		});
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		swingTimer.stop();
	}

	protected void commitTimer() {
		PamConnection con = dbControl.getConnection();
		if (con == null) return;
		commitNow(con);
	}

	public boolean checkCommit(PamConnection pamCon) {
		unComitted ++;
		if (unComitted >= COMMIT_ROWS) {
			commitNow(pamCon);
		}
		return true;
	}
	
	public synchronized boolean commitNow(PamConnection pamCon) {
		if (unComitted == 0) {
			return true;
		}
		unComitted = 0;
		
		if (dbControl.dbParameters.getUseAutoCommit()) {
			return true;
		}
		if (pamCon == null || pamCon.getConnection() == null) {
			return false;
		}
		try {
			if (!pamCon.getConnection().getAutoCommit()) {
				pamCon.getConnection().commit();
			}
		} catch (SQLException e) {
//			e.printStackTrace();
			reportError(e);
			return false;
		}
		lastCommitTime = PamCalendar.getTimeInMillis();
		swingTimer.restart();
		reportError(null);
		return true;
	}

	/**
	 * Report or cancel a warning for the database commit error.
	 * @param e
	 */
	private void reportError(SQLException e) {
		if (e == null) {
			if (commitWarning != null) {
				WarningSystem.getWarningSystem().removeWarning(commitWarning);
				commitWarning = null;
			}
		}
		else {
			if (commitWarning == null){
				commitWarning = new PamWarning("Database System Error", e.getMessage(), 2);
				WarningSystem.getWarningSystem().addWarning(commitWarning);
			}
			else {
				commitWarning.setWarningMessage(e.getMessage());
			}
		}
	}
}
