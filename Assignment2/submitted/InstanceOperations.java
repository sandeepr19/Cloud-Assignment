package com;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AttachVolumeRequest;
import com.amazonaws.services.ec2.model.CreateImageRequest;
import com.amazonaws.services.ec2.model.CreateImageResult;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.DetachVolumeRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

public class InstanceOperations {

	public static void terminateInstance(AmazonEC2 ec2, List<String> instanceIds) {
		TerminateInstancesRequest stopIR = new TerminateInstancesRequest(
				instanceIds);
		ec2.terminateInstances(stopIR);

	}

	public static String getUserINfo(AmazonS3Client s3, String username, String bucketName)
			throws IOException {
		S3Object object = s3.getObject(new GetObjectRequest(bucketName,
				username));
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				object.getObjectContent()));

		return reader.readLine();

	}

	public static String createInstance(AmazonEC2 ec2, String imageId) {
		RunInstancesRequest rir = new RunInstancesRequest(imageId, 1, 1);
		RunInstancesResult result = ec2.runInstances(rir);
		List<Instance> resultInstance = result.getReservation().getInstances();
		for (Instance ins : resultInstance) {
			System.out.println("New instance has been created: "
					+ ins.getInstanceId());
		}
		return resultInstance.get(0).getInstanceId();
	}

	public static void detachVolumeRequest(AmazonEC2 ec2,
			InstanceProperty instanceProperty) {
		DetachVolumeRequest dvr = new DetachVolumeRequest();
		dvr.setVolumeId(instanceProperty.getVolumeId());
		dvr.setInstanceId(instanceProperty.getInstanceId());
		ec2.detachVolume(dvr);
	}

	public static String createVolumeRequest(AmazonEC2 ec2) {
		CreateVolumeRequest cvr = new CreateVolumeRequest();
		cvr.setAvailabilityZone("us-east-1a");
		cvr.setSize(10); // size = 10 gigabytes
		CreateVolumeResult volumeResult = ec2.createVolume(cvr);
		String createdVolumeId = volumeResult.getVolume().getVolumeId();
		return createdVolumeId;
	}

	public static void attachVolumeRequest(AmazonEC2 ec2,
			InstanceProperty instanceProperty) {
		AttachVolumeRequest avr = new AttachVolumeRequest();
		avr.setVolumeId(instanceProperty.getVolumeId());
		avr.setInstanceId(instanceProperty.getInstanceId());
		avr.setDevice("/dev/sdf");
		ec2.attachVolume(avr);

	}

	public static void createImage(AmazonEC2 ec2,
			InstanceProperty instanceProperty) {
		CreateImageRequest cir = new CreateImageRequest();
		cir.setInstanceId(instanceProperty.getInstanceId());
		cir.setName(instanceProperty.getUserName() + Math.random());
		CreateImageResult createImageResult = ec2.createImage(cir);
		String createdImageId = createImageResult.getImageId();
		instanceProperty.setImageId(createdImageId);

	}

	public static void persistInS3(AmazonS3Client s3,
			InstanceProperty instanceProperty) throws IOException {
		File file = File.createTempFile(instanceProperty.getFileName(), ".txt");
		file.deleteOnExit();
		Writer writer = new OutputStreamWriter(new FileOutputStream(file));
		writer.write(instanceProperty.getUserName() + ":" + instanceProperty.getInstanceId() + ":"
				+ instanceProperty.getVolumeId() + ":"
				+ instanceProperty.getImageId() + ":" + instanceProperty.getElasticIp());
		writer.close();
		try {
			s3.putObject(new PutObjectRequest(instanceProperty.getBucketName(),
					instanceProperty.getUserName(), file));
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println("bucket created");

		}
	}
}
