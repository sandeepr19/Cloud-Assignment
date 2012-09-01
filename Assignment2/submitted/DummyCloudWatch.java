package com;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AllocateAddressResult;
import com.amazonaws.services.ec2.model.AssociateAddressRequest;
import com.amazonaws.services.ec2.model.AttachVolumeRequest;
import com.amazonaws.services.ec2.model.CreateImageRequest;
import com.amazonaws.services.ec2.model.CreateImageResult;
import com.amazonaws.services.ec2.model.DeregisterImageRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DetachVolumeRequest;
import com.amazonaws.services.ec2.model.DisassociateAddressRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.s3.AmazonS3Client;

/**
 * Simple demo that uses java.util.Timer to schedule a task to execute once 5
 * seconds have passed.
 */

public class DummyCloudWatch {

	Timer timer;
	AmazonEC2 ec2;
	AmazonS3Client s3;
	String bucketName = "cloud-bucket";
	String key = "cloud-key.txt";
	String fileName = "temp";
	String imageId = "ami-ab844dc2";
	String userName = "sandeep";
	ArrayList<InstanceProperty> listOfInstances;
	AWSCredentials credentials;
	int size = 2;
	int i = 1;
	int threshold = 30;

	public DummyCloudWatch() {
		try {
			credentials = new PropertiesCredentials(
					DummyCloudWatch.class
							.getResourceAsStream("AwsCredentials.properties"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ec2 = new AmazonEC2Client(credentials);
		s3 = new AmazonS3Client(credentials);
		timer = new Timer();
		timer.schedule(new Admin(), 0, 1 * 1000);
	}

	public static void main(String[] args) {
		new DummyCloudWatch();
	}

	class Admin extends TimerTask {
		private boolean firstTime = false;

		public void createInfrastructure() throws IOException {
			listOfInstances = new ArrayList<InstanceProperty>();
			for (int i = 0; i < 2; i++) {
				InstanceProperty instanceProperty = new InstanceProperty();
				instanceProperty.setInstanceId(InstanceOperations
						.createInstance(ec2, imageId));
				listOfInstances.add(instanceProperty);

			}

			List<String> instanceIds = new LinkedList<String>();
			for (InstanceProperty instanceProperty : listOfInstances) {
				instanceProperty.setUserName(userName + i);
				instanceProperty.setBucketName(bucketName + i);
				instanceProperty.setKey(key + i);
				instanceProperty.setFileName(fileName + i);
				instanceProperty.setVolumeId(InstanceOperations
						.createVolumeRequest(ec2));
				AllocateAddressResult elasticResult = ec2.allocateAddress();
				String elasticIp = elasticResult.getPublicIp();
				instanceProperty.setElasticIp(elasticIp);
				InstanceOperations.attachVolumeRequest(ec2, instanceProperty);
				InstanceOperations.detachVolumeRequest(ec2, instanceProperty);
				InstanceOperations.createImage(ec2, instanceProperty);
				instanceIds.add(instanceProperty.getInstanceId());
				InstanceOperations.persistInS3(s3, instanceProperty);
				i++;
			}
			InstanceOperations.terminateInstance(ec2, instanceIds);

		}

		public void run() {
			if (!firstTime) {
				try {
					createInfrastructure();
					firstTime = true;
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				timer = new Timer();
				timer.schedule(new CreationOfInstances(), 1 * 1000);
				timer.schedule(new MonitorInstances(), 600 * 1000, 120 * 1000);
				timer.schedule(new DeletionOfInstances(), 1800 * 1000);

			}
		}
	}

	class CreationOfInstances extends TimerTask {
		Timer timer;

		@Override
		public void run() {
			String instanceData = null;
			ArrayList<InstanceProperty> listOfInstances = new ArrayList<InstanceProperty>();
			for (int j = 0; j < size; j++) {
				String suffix = Integer.toString(j + 1);
				try {
					instanceData = InstanceOperations.getUserINfo(s3, userName
							+ suffix, bucketName + suffix);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				InstanceProperty instanceProperty = new InstanceProperty();
				instanceProperty.setBucketName(bucketName + suffix);
				instanceProperty.setUserName(userName + suffix);
				instanceProperty.setVolumeId(instanceData.split(":")[2]);
				instanceProperty.setImageId(instanceData.split(":")[3]);
				instanceProperty.setElasticIp(instanceData.split(":")[4]);
				listOfInstances.add(instanceProperty);
			}
			for (InstanceProperty instanceProperty : listOfInstances) {
				RunInstancesRequest rir = new RunInstancesRequest(
						instanceProperty.getImageId(), 1, 1).withKeyName(
						"amzon").withInstanceType("t1.micro");
				RunInstancesResult result = ec2.runInstances(rir);
				List<Instance> resultInstance = result.getReservation()
						.getInstances();
				String createdInstanceId = null;
				Instance createdInstance = null;
				for (Instance ins : resultInstance) {
					createdInstanceId = ins.getInstanceId();
					createdInstance = ins;
					System.out.println("New instance has been created: "
							+ ins.getInstanceId());
				}
				instanceProperty.setInstanceId(resultInstance.get(0)
						.getInstanceId());

				String publicdnsName = null;
				while (publicdnsName == null) {
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					DescribeInstancesResult describeInstancesRequest = ec2
							.describeInstances();
					List<Reservation> reservations = describeInstancesRequest
							.getReservations();
					Set<Instance> instances = new HashSet<Instance>();
					for (Reservation reservation : reservations) {
						instances.addAll(reservation.getInstances());
					}
				}

				AttachVolumeRequest ac = new AttachVolumeRequest()
						.withInstanceId(createdInstanceId)
						.withVolumeId(instanceProperty.getVolumeId())
						.withDevice("/dev/sdf");
				AssociateAddressRequest associateAddressRequest = new AssociateAddressRequest();
				associateAddressRequest.setInstanceId(createdInstanceId);
				associateAddressRequest.setPublicIp(instanceProperty
						.getElasticIp());
				ec2.associateAddress(associateAddressRequest);
				ec2.attachVolume(ac);
				ec2.shutdown();
				try {
					InstanceOperations.persistInS3(s3, instanceProperty);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

		}

	}

	class MonitorInstances extends TimerTask {

		public void run() {

			String instanceData = null;
			ArrayList<InstanceProperty> listOfInstances = new ArrayList<InstanceProperty>();
			ArrayList<String> listOfInstanceId = new ArrayList<String>();
			for (int j = 0; j < size; j++) {
				String suffix = Integer.toString(j + 1);
				try {
					instanceData = InstanceOperations.getUserINfo(s3, userName
							+ suffix, bucketName + suffix);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				InstanceProperty instanceProperty = new InstanceProperty();
				instanceProperty.setBucketName(bucketName + suffix);
				instanceProperty.setUserName(userName + suffix);
				instanceProperty.setInstanceId(instanceData.split(":")[1]);
				instanceProperty.setVolumeId(instanceData.split(":")[2]);
				instanceProperty.setImageId(instanceData.split(":")[3]);
				instanceProperty.setBelowTHreshold(instanceData.split(":")[5]);
				listOfInstances.add(instanceProperty);
			}

			for (InstanceProperty instanceProperty : listOfInstances) {
				try {
					if (instanceProperty.getInstanceId().equalsIgnoreCase(""))
						continue;
					String instanceId = instanceProperty.getInstanceId();
					AllocateAddressResult elasticResult = ec2.allocateAddress();
					String elasticIp = elasticResult.getPublicIp();
					System.out.println("New elastic IP: " + elasticIp);
					AssociateAddressRequest aar = new AssociateAddressRequest();
					aar.setInstanceId(instanceId);
					aar.setPublicIp(elasticIp);
					ec2.associateAddress(aar);
					DisassociateAddressRequest dar = new DisassociateAddressRequest();
					dar.setPublicIp(elasticIp);
					ec2.disassociateAddress(dar);
					AmazonCloudWatchClient cloudWatch = new AmazonCloudWatchClient(
							credentials);
					GetMetricStatisticsRequest statRequest = new GetMetricStatisticsRequest();
					statRequest.setNamespace("AWS/EC2");
					statRequest.setPeriod(60);
					ArrayList<String> stats = new ArrayList<String>();
					stats.add("Average");
					stats.add("Sum");
					statRequest.setStatistics(stats);
					statRequest.setMetricName("CPUUtilization");
					GregorianCalendar calendar = new GregorianCalendar(
							TimeZone.getTimeZone("UTC"));
					calendar.add(GregorianCalendar.SECOND,
							-1 * calendar.get(GregorianCalendar.SECOND)); // 1
					Date endTime = calendar.getTime();
					calendar.add(GregorianCalendar.MINUTE, -10); // 10 minutes
					Date startTime = calendar.getTime();
					statRequest.setStartTime(startTime);
					statRequest.setEndTime(endTime);
					ArrayList<Dimension> dimensions = new ArrayList<Dimension>();
					dimensions.add(new Dimension().withName("InstanceId")
							.withValue(instanceId));
					statRequest.setDimensions(dimensions);
					GetMetricStatisticsResult statResult = cloudWatch
							.getMetricStatistics(statRequest);
					System.out.println(statResult.toString());
					List<Datapoint> dataList = statResult.getDatapoints();
					Double averageCPU = null;
					Date timeStamp = null;
					for (Datapoint data : dataList) {
						averageCPU = data.getAverage();
						timeStamp = data.getTimestamp();
						System.out
								.println("Average CPU utlilization for last 10 minutes: "
										+ averageCPU);
						System.out
								.println("Totl CPU utlilization for last 10 minutes: "
										+ data.getSum());

						if (averageCPU < threshold) {
							instanceProperty.setInstanceId("");
							listOfInstanceId.add(instanceProperty
									.getInstanceId());

						}
					}
					try {
						InstanceOperations.persistInS3(s3, instanceProperty);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				} catch (AmazonServiceException ase) {
					System.out.println("Caught Exception: " + ase.getMessage());
					System.out.println("Reponse Status Code: "
							+ ase.getStatusCode());
					System.out.println("Error Code: " + ase.getErrorCode());
					System.out.println("Request ID: " + ase.getRequestId());
				}
			}
			TerminateInstancesRequest stopIR = new TerminateInstancesRequest(
					listOfInstanceId);
			ec2.terminateInstances(stopIR);
			ec2.shutdown();

		}

	}

	class DeletionOfInstances extends TimerTask {
		Timer timer;

		@Override
		public void run() {

			String instanceData = null;
			ArrayList<InstanceProperty> listOfInstances = new ArrayList<InstanceProperty>();
			for (int j = 0; j < size; j++) {
				String suffix = Integer.toString(j + 1);
				try {
					instanceData = InstanceOperations.getUserINfo(s3, userName
							+ suffix, bucketName + suffix);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				InstanceProperty instanceProperty = new InstanceProperty();
				instanceProperty.setBucketName(bucketName + suffix);
				instanceProperty.setUserName(userName + suffix);
				instanceProperty.setInstanceId(instanceData.split(":")[1]);
				instanceProperty.setVolumeId(instanceData.split(":")[2]);
				instanceProperty.setImageId(instanceData.split(":")[3]);
				instanceProperty.setElasticIp(instanceData.split(":")[4]);
				listOfInstances.add(instanceProperty);
			}
			List<String> listOfInstanceId = new ArrayList<String>();
			for (InstanceProperty instanceProperty : listOfInstances) {
				DisassociateAddressRequest disassociateAddressRequest = new DisassociateAddressRequest();
				disassociateAddressRequest.setPublicIp(instanceProperty
						.getElasticIp());
				ec2.disassociateAddress(disassociateAddressRequest);
				DetachVolumeRequest ac = new DetachVolumeRequest()
						.withInstanceId(instanceProperty.getInstanceId())
						.withVolumeId(instanceProperty.getVolumeId());
				ec2.detachVolume(ac);
				if (instanceProperty.getImageId() != null
						&& !instanceProperty.getImageId().equals(" ")) {
					DeregisterImageRequest dir = new DeregisterImageRequest()
							.withImageId(instanceProperty.getImageId());
					ec2.deregisterImage(dir);
				}
				CreateImageRequest cir = new CreateImageRequest();
				cir.setInstanceId(instanceProperty.getInstanceId());
				cir.setName(instanceProperty.getUserName() + Math.random());
				CreateImageResult createImageResult = ec2.createImage(cir);
				String createdImageId = createImageResult.getImageId();
				instanceProperty.setImageId(createdImageId);
				System.out.println("Sent creating AMI request. AMI id="
						+ createdImageId);
				listOfInstanceId.add(instanceProperty.getInstanceId());
				try {
					InstanceOperations.persistInS3(s3, instanceProperty);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			TerminateInstancesRequest stopIR = new TerminateInstancesRequest(
					listOfInstanceId);
			ec2.terminateInstances(stopIR);
			ec2.shutdown();

		}
	}

}