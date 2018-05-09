package sensor;

import java.awt.geom.Point2D;
import java.io.Serializable;

import messages.SensorState;

public class SensorImpl extends MeasurementData implements Serializable {
	
	// Explicit declaration of serialVersionUID, hence InvalidClassExceptions will never be thrown during deserialization
	private static final long serialVersionUID = 1L;
		
	// class attributes
	private final int sensor_m_history_array_size = 24;
	protected int sensorID;
	protected Point2D.Float coordinates = null;
	protected String softwareImageID = null;
	protected SensorState sensorState = SensorState.PRE_OPERATIONAL;
	protected MeasurementData[] sensor_m_history= new MeasurementData[sensor_m_history_array_size];
	protected int numberOfMeasurements = 0;
	
	// Default SensorImpl class constructor - for the client side
	public SensorImpl(int sensorID) {
		super();
		this.sensorID = sensorID;
	}

	// Overloaded SensorImpl class constructor - for the server side
	public SensorImpl(int sensorID, Point2D.Float coordinates, String softwareImageID) {
		super();
		this.sensorID = sensorID;
		this.coordinates = coordinates;
		this.softwareImageID = softwareImageID;
	}
	
	public void resetSensor() {
		this.sensor_m_history = new MeasurementData[this.sensor_m_history_array_size];
		setNumberOfMeasurements(0);
		setSensorState(SensorState.PRE_OPERATIONAL);
	}
	
	public boolean addMeasurement(float pm25, float pm10, int humidity, int temperature, int pressure) {
		boolean success = false;
		if (getSensorState() == SensorState.OPERATIONAL) {
			try {
				int temp_numberOfMeasurements = getNumberOfMeasurements();
				
				// overwrite the MeasurementData class instance to get current timestamp
				this.sensor_m_history[temp_numberOfMeasurements] = new MeasurementData();
				
				this.sensor_m_history[temp_numberOfMeasurements].setPm25(pm25);
				this.sensor_m_history[temp_numberOfMeasurements].setPm10(pm10);
				this.sensor_m_history[temp_numberOfMeasurements].setHumidity(humidity);
				this.sensor_m_history[temp_numberOfMeasurements].setTemperature(temperature);
				this.sensor_m_history[temp_numberOfMeasurements].setPressure(pressure);
				setNumberOfMeasurements(temp_numberOfMeasurements + 1);
				
				// Measurements is successful
				success = true;
			} catch (ArrayIndexOutOfBoundsException AIOOBex) {
				System.out.println("Error: Sensor Measurement History has exceeded it allowed size, Sensor goes to the DAMAGED state");
				setSensorState(SensorState.DAMAGED);
				AIOOBex.printStackTrace();
			}
		} else {
			int temp_numberOfMeasurements = getNumberOfMeasurements();
			
			// overwrite the MeasurementData class instance to get current timestamp
			this.sensor_m_history[temp_numberOfMeasurements] = new MeasurementData();
			
			setNumberOfMeasurements(temp_numberOfMeasurements + 1);
			
			//cannot measure, because the sensor is not in the OPERETIONAL state
			success = false;
		}
		return success;
	}
	
	public MeasurementData readLastMeasurementData() {
		int temp_numberOfMeasurements = getNumberOfMeasurements();
		return sensor_m_history[temp_numberOfMeasurements];
	}
	
	public MeasurementData[] readMeasurementHistory() {
		return sensor_m_history;
	}
	
	public int getSensorID() {
		return sensorID;
	}
	public void setSensorID(int sensorID) {
		this.sensorID = sensorID;
	}
	public Point2D.Float getCoordinates() {
		return coordinates;
	}
	public void setCoordinates(Point2D.Float coordinates) {
		this.coordinates = coordinates;
	}
	public String getSoftwareImageID() {
		return softwareImageID;
	}
	public void setSoftwareImageID(String softwareImageID) {
		this.softwareImageID = softwareImageID;
	}
	public SensorState getSensorState() {
		return sensorState;
	}
	public void setSensorState(SensorState sensorState) {
		this.sensorState = sensorState;
	}
	
	public int getNumberOfMeasurements() {
		return numberOfMeasurements;
	}

	public void setNumberOfMeasurements(int numberOfMeasurements) {
		this.numberOfMeasurements = numberOfMeasurements;
	}

}
