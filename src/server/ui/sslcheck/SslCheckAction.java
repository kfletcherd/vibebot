package server.ui.sslcheck;

import server.model.Server;
import server.model.ServerStore;
import util.tls.CertProbe;
import util.tls.CertProbeResult;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingWorker;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Orchestrates a one-click SSL certificate check across every server in a
 * {@link ServerStore} that has a non-empty hostname.
 *
 * <p>The check runs off the EDT inside a {@link SwingWorker}. While in
 * flight, any wired trigger button is disabled. When the worker completes,
 * the resulting rows are shown in {@link SslCheckReportDialog} and the
 * trigger is re-enabled.
 */
public final class SslCheckAction {

	private final JFrame ownerFrame;
	private final ServerStore store;

	/**
	 * @param ownerFrame the parent frame used to parent the modal report dialog
	 * @param store the server store whose entries to probe
	 */
	public SslCheckAction(JFrame ownerFrame, ServerStore store) {
		if (ownerFrame == null) throw new IllegalArgumentException("ownerFrame must not be null");
		if (store == null) throw new IllegalArgumentException("store must not be null");
		this.ownerFrame = ownerFrame;
		this.store = store;
	}

	/**
	 * Wires {@code trigger} so clicking it kicks off a check; the button is
	 * disabled while the check runs and re-enabled when the dialog closes.
	 * Calling this multiple times with the same button is safe.
	 */
	public void attachTo(JButton trigger) {
		if (trigger == null) throw new IllegalArgumentException("trigger must not be null");
		trigger.addActionListener(e -> startRun(trigger));
	}

	/**
	 * Triggers a check programmatically with no associated trigger button.
	 */
	public void run() {
		startRun(null);
	}

	private void startRun(JButton trigger) {
		if (trigger != null) {
			trigger.setEnabled(false);
		}
		new ProbeWorker(trigger).execute();
	}

	private List<SslCheckReportRow> probeAll() {
		Instant now = Instant.now();
		CertProbe probe = new CertProbe();
		List<SslCheckReportRow> rows = new ArrayList<>();
		for (Server server : store.servers()) {
			String hostname = server.hostname();
			if (hostname == null || hostname.isBlank()) {
				continue;
			}
			CertProbeResult result = probe.probe(hostname);
			rows.add(SslCheckReportRow.from(server.shortName(), result, now));
		}
		return rows;
	}

	/**
	 * Background worker that runs the probe batch and then shows the dialog on
	 * the EDT.
	 */
	private final class ProbeWorker extends SwingWorker<List<SslCheckReportRow>, Void> {

		private final JButton trigger;

		ProbeWorker(JButton trigger) {
			this.trigger = trigger;
		}

		@Override
		protected List<SslCheckReportRow> doInBackground() {
			return probeAll();
		}

		@Override
		protected void done() {
			try {
				List<SslCheckReportRow> rows = get();
				SslCheckReportDialog.show(ownerFrame, rows);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				System.err.println("SSL check interrupted: " + e.getMessage());
			} catch (ExecutionException e) {
				System.err.println("SSL check failed: " + e.getCause());
			} finally {
				if (trigger != null) {
					trigger.setEnabled(true);
				}
			}
		}
	}
}
