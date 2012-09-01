

/*
 * Copyright 2010 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * Modified by Sambit Sahu
 * Modified by Kyung-Hwa Kim (kk2515@columbia.edu)
 * 
 * 
 */
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AttachVolumeRequest;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeKeyPairsResult;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;

public class AwsSample {

	/*
	 * Important: Be sure to fill in your AWS access credentials in the
	 * AwsCredentials.properties file before you try to run this sample.
	 * http://aws.amazon.com/security-credentials
	 */

	static AmazonEC2 ec2;

	public static void main(String[] args) throws Exception {
		String securityGroupName = "securityGroup";
		// String programmaticSecGrpName="trial security group instantiation";
		String programmaticSecGrpName = "trialSecurityGroup";
		CreateSecurityGroupRequest secGrpReq = new CreateSecurityGroupRequest(
				programmaticSecGrpName, "ssh,http,https enabled");
		System.out.println("Created security group request");

		AWSCredentials credentials = new PropertiesCredentials(
				AwsSample.class
						.getResourceAsStream("AwsCredentials.properties"));

		/*********************************************
		 * 
		 * #1 Create Amazon Client object
		 * 
		 *********************************************/
		System.out.println("#1 Create Amazon Client object");
		ec2 = new AmazonEC2Client(credentials);

		// related to groups starts here
		DescribeSecurityGroupsResult descSecGrpReslt = ec2
				.describeSecurityGroups();
		CreateSecurityGroupResult sdf = ec2.createSecurityGroup(secGrpReq);

		List<SecurityGroup> securityGroups = descSecGrpReslt
				.getSecurityGroups();
		List<IpPermission> ipPermission = null;
		for (SecurityGroup sg : securityGroups) {
			ipPermission = sg.getIpPermissions();
			System.out.println(sg.getGroupName());
			for (IpPermission i : ipPermission) {
				System.out.println("ipProtocol " + i.getIpProtocol()
						+ " fromPort " + i.getFromPort() + " toPort "
						+ i.getToPort());
			}
			if (sg.getGroupName().equals(programmaticSecGrpName)) {
				DeleteSecurityGroupRequest delete = new DeleteSecurityGroupRequest(
						sg.getGroupName());
				ec2.deleteSecurityGroup(delete);
			}
		}
		// Permission list
		List<IpPermission> permissionList = new ArrayList<IpPermission>();

		IpPermission permissionSSH = new IpPermission();
		permissionSSH.setIpProtocol("tcp");
		permissionSSH.setFromPort(22);
		permissionSSH.setToPort(22);
		permissionList.add(permissionSSH);

		IpPermission permissionHttp = new IpPermission();
		permissionHttp.setIpProtocol("tcp");
		permissionHttp.setFromPort(80);
		permissionHttp.setToPort(80);
		permissionList.add(permissionHttp);

		AuthorizeSecurityGroupIngressRequest ingrReqSSH = new AuthorizeSecurityGroupIngressRequest();
		ingrReqSSH.setFromPort(22);
		ingrReqSSH.setToPort(22);
		ingrReqSSH.setIpProtocol("tcp");
		ingrReqSSH.setCidrIp("0.0.0.0/00");
		ingrReqSSH.setGroupName(programmaticSecGrpName);
		ec2.authorizeSecurityGroupIngress(ingrReqSSH);

		AuthorizeSecurityGroupIngressRequest ingrReqHTTP = new AuthorizeSecurityGroupIngressRequest();
		ingrReqHTTP.setFromPort(80);
		ingrReqHTTP.setToPort(80);
		ingrReqHTTP.setCidrIp("0.0.0.0/00");
		ingrReqHTTP.setGroupName(programmaticSecGrpName);
		ingrReqHTTP.setIpProtocol("tcp");
		ec2.authorizeSecurityGroupIngress(ingrReqHTTP);

		AuthorizeSecurityGroupIngressRequest ingrReqHTTPS = new AuthorizeSecurityGroupIngressRequest();
		ingrReqHTTPS.setFromPort(443);
		ingrReqHTTPS.setToPort(443);
		ingrReqHTTPS.setCidrIp("0.0.0.0/00");
		ingrReqHTTPS.setGroupName(programmaticSecGrpName);
		ingrReqHTTPS.setIpProtocol("tcp");
		ec2.authorizeSecurityGroupIngress(ingrReqHTTPS);

		// Check security groups again
		System.out
				.println("Checking security groups after initialising new security group- "
						+ programmaticSecGrpName);
		descSecGrpReslt = ec2.describeSecurityGroups();
		securityGroups = descSecGrpReslt.getSecurityGroups();
		for (SecurityGroup sg : securityGroups) {
			ipPermission = sg.getIpPermissions();
			System.out.println(sg.getGroupName());
			for (IpPermission i : ipPermission) {
				System.out.println("ipProtocol " + i.getIpProtocol()
						+ " from port " + i.getFromPort() + " toPort "
						+ i.getToPort());
			}
		}

		// List to be passed to the setSecurityGroups function
		List<String> secGroupList = new ArrayList<String>();
		secGroupList.add(programmaticSecGrpName);

		// this ends here related to groups

		CreateKeyPairRequest c = new CreateKeyPairRequest()
				.withKeyName("abc431124");
		CreateKeyPairResult keyresult = ec2.createKeyPair(c);

		try {
			// Create file
			FileWriter fstream = new FileWriter("abc431123.pem");
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(keyresult.getKeyPair().getKeyMaterial());
			// Close the output stream
			out.close();
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
		try {

			/*********************************************
			 * 
			 * #2 Describe Availability Zones.
			 * 
			 *********************************************/

			System.out.println("#2 Describe Availability Zones.");
			DescribeAvailabilityZonesResult availabilityZonesResult = ec2
					.describeAvailabilityZones();
			System.out.println("You have access to "
					+ availabilityZonesResult.getAvailabilityZones().size()
					+ " Availability Zones.");

			/*********************************************
			 * 
			 * #3 Describe Available Images
			 * 
			 *********************************************/
			System.out.println("#3 Describe Available Images");
			DescribeImagesResult dir = ec2.describeImages();
			List<Image> images = dir.getImages();
			System.out.println("You have " + images.size() + " Amazon images");

			/*********************************************
			 * 
			 * #4 Describe Key Pair
			 * 
			 *********************************************/
			System.out.println("#9 Describe Key Pair");
			DescribeKeyPairsResult dkr = ec2.describeKeyPairs();
			System.out.println(dkr.toString());

			/*********************************************
			 * 
			 * #5 Describe Current Instances
			 * 
			 *********************************************/
			System.out.println("#4 Describe Current Instances");
			DescribeInstancesResult describeInstancesRequest = ec2
					.describeInstances();
			List<Reservation> reservations = describeInstancesRequest
					.getReservations();
			Set<Instance> instances = new HashSet<Instance>();
			// add all instances to a Set.
			for (Reservation reservation : reservations) {
				instances.addAll(reservation.getInstances());
			}

			System.out.println("You have " + instances.size()
					+ " Amazon EC2 instance(s).");
			for (Instance ins : instances) {

				// instance id
				String instanceId = ins.getInstanceId();

				// instance state
				InstanceState is = ins.getState();
				System.out.println(instanceId + " " + is.getName());
			}

			/*********************************************
			 * 
			 * #6 Create an Instance
			 * 
			 *********************************************/
			System.out.println("#5 Create an Instance");
			String imageId = "ami-ab844dc2"; // Basic 32-bit Amazon Linux AMI
			int minInstanceCount = 1; // create 1 instance
			int maxInstanceCount = 1;
			ArrayList<String> security = new ArrayList<String>();
			security.add(programmaticSecGrpName);
			RunInstancesRequest rir = new RunInstancesRequest(imageId,
					minInstanceCount, maxInstanceCount).withInstanceType(
					"t1.micro").withSecurityGroups(security);
			rir.setKeyName("abc431124");
			RunInstancesResult result = ec2.runInstances(rir);

			// get instanceId from the result
			List<Instance> resultInstance = result.getReservation()
					.getInstances();
			String createdInstanceId = null;
			for (Instance ins : resultInstance) {
				createdInstanceId = ins.getInstanceId();
				System.out.println("New instance has been created: "
						+ ins.getInstanceId());
			}

			List<String> publicDNSAddressOfInstances = new ArrayList<String>();

			for (Instance instance : result.getReservation().getInstances()) {

				// add tags for easy identification
				List<String> resources = new LinkedList<String>();
				List<Tag> tags = new LinkedList<Tag>();
				resources.add(instance.getInstanceId());
				tags.add(new Tag("Name", "Sandeep"));
				tags.add(new Tag("Source", "AmazonEC2_Java_Api"));
				ec2.createTags(new CreateTagsRequest(resources, tags));

				// remove terminated and shutting-down instances
				if (instance.getState().getName()
						.equalsIgnoreCase("shutting-down")
						|| instance.getState().getName()
								.equalsIgnoreCase("terminated")) {
					continue;
				}

				while (true) {
					// if pending - wait, else - get out of the while loop.
					if (instance.getState().getName()
							.equalsIgnoreCase("pending")) {
						System.out.println("[" + new Date() + "] Instance "
								+ instance.getInstanceId() + " is still in '"
								+ instance.getState().getName() + "' state");
						System.out.println(" Sleeping for 30 seconds ");
						for (int sec = 30; sec > 0; sec--) {
							System.out.print(sec + ", ");
							Thread.sleep(1000);
						}
						DescribeInstancesRequest instancesRequest = new DescribeInstancesRequest();
						instancesRequest
								.setInstanceIds(new ArrayList<String>());
						instancesRequest.getInstanceIds().add(
								instance.getInstanceId());

						DescribeInstancesResult instancesResult = ec2
								.describeInstances(instancesRequest);
						instance = instancesResult.getReservations().get(0)
								.getInstances().get(0);
						continue;
					} else if (instance.getState().getName()
							.equalsIgnoreCase("running")) {
						break;
					}
				}

				System.out.println("[" + new Date() + "] Instance "
						+ instance.getInstanceId() + " is now in '"
						+ instance.getState().getName() + "' state");
				System.out.println("Public DNS address of the instance "
						+ instance.getInstanceId() + " is --->  "
						+ instance.getPublicDnsName());
				publicDNSAddressOfInstances.add(instance.getPublicDnsName());

			}

			/* create a volume and attach it to the instance */
			CreateVolumeRequest cvr = new CreateVolumeRequest();
			cvr.setAvailabilityZone(resultInstance.get(0).getPlacement()
					.getAvailabilityZone());
			cvr.setSize(10); // size = 10 gigabytes

			CreateVolumeResult volumeResult = ec2.createVolume(cvr);
			String createdVolumeId = volumeResult.getVolume().getVolumeId();

			// user.setVolumeId(createdVolumeId);

			/* Attaching volume to system */
			AttachVolumeRequest avr = new AttachVolumeRequest();
			avr.setVolumeId(createdVolumeId);
			avr.setInstanceId(resultInstance.get(0).getInstanceId());
			avr.setDevice("/dev/sdf");
			ec2.attachVolume(avr);

			// System.out.println("[AWS] Attached volume for " + user);

			/*********************************************
			 * 
			 * #7 Create a 'tag' for the new instance.
			 * 
			 *********************************************/
			System.out.println("#6 Create a 'tag' for the new instance.");
			List<String> resources = new LinkedList<String>();
			List<Tag> tags = new LinkedList<Tag>();
			Tag nameTag = new Tag("Name", "MyFirstInstance");

			resources.add(createdInstanceId);
			tags.add(nameTag);

			CreateTagsRequest ctr = new CreateTagsRequest(resources, tags);
			ec2.createTags(ctr);

			/*********************************************
			 * 
			 * #8 Stop/Start an Instance
			 * 
			 *********************************************/
			System.out.println("#7 Stop the Instance");
			List<String> instanceIds = new LinkedList<String>();
			instanceIds.add(createdInstanceId);

			// stop
			StopInstancesRequest stopIR = new StopInstancesRequest(instanceIds);
			// ec2.stopInstances(stopIR);

			// start
			StartInstancesRequest startIR = new StartInstancesRequest(
					instanceIds);
			// ec2.startInstances(startIR);

			/*********************************************
			 * 
			 * #9 Terminate an Instance
			 * 
			 *********************************************/
			System.out.println("#8 Terminate the Instance");
			TerminateInstancesRequest tir = new TerminateInstancesRequest(
					instanceIds);
			// ec2.terminateInstances(tir);

			/*********************************************
			 * 
			 * #10 shutdown client object
			 * 
			 *********************************************/
			ec2.shutdown();

		} catch (AmazonServiceException ase) {
			System.out.println("Caught Exception: " + ase.getMessage());
			System.out.println("Reponse Status Code: " + ase.getStatusCode());
			System.out.println("Error Code: " + ase.getErrorCode());
			System.out.println("Request ID: " + ase.getRequestId());
		}

	}

}
