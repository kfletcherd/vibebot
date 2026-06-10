/**
 * Server-feature UI glue for the "Check SSL Certs" toolbar action.
 *
 * <p>The orchestrator {@link server.ui.sslcheck.SslCheckAction} is the only
 * type in this subpackage with cross-package visibility; all other classes
 * (report row, dialog, table model) are package-private and reached only
 * from inside {@code SslCheckAction}. Classes in this package depend on
 * {@code util.tls} for the actual probing and on {@code server.model} for
 * the server list.
 */
package server.ui.sslcheck;
