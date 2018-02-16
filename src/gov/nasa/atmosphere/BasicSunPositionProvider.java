package gov.nasa.atmosphere;

import gov.nasa.atmosphere.SunPositionProvider;
import gov.nasa.worldwind.geom.LatLon;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * @author Michael de Hoog
 * @version $Id$
 */
public class BasicSunPositionProvider implements SunPositionProvider {
	private LatLon position;
	private Calendar calendar;

	public BasicSunPositionProvider() {
		calendar = new GregorianCalendar();
		updatePosition();

		Thread thread = new Thread(new Runnable() {
			public void run() {
				while (true) {
					try {
						Thread.sleep(60000);
					} catch (InterruptedException ignore) {
					}
					calendar.setTimeInMillis(System.currentTimeMillis());
					updatePosition();
				}
			}
		});
		thread.setDaemon(true);
		thread.start();
	}

	private synchronized void updatePosition() {
		position = SunCalculator.subsolarPoint(calendar);
	}

	public synchronized LatLon getPosition() {
		return position;
	}
}
