// Package externalstorage shows how to offload large Payloads to external storage.
package externalstorage

import (
	"context"
	"log"

	"cloud.google.com/go/storage"
	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/s3"
	"go.temporal.io/sdk/contrib/aws/s3driver"
	"go.temporal.io/sdk/contrib/aws/s3driver/awssdkv2"
	"go.temporal.io/sdk/contrib/gcp/gcsdriver"
	"go.temporal.io/sdk/contrib/gcp/gcsdriver/gcssdk"
	"go.temporal.io/sdk/converter"
)

// CreateS3Driver creates a storage driver for an Amazon S3 bucket.
func CreateS3Driver() converter.StorageDriver {
	// @@@SNIPSTART go-external-storage-s3-driver
	cfg, err := config.LoadDefaultConfig(context.Background(),
		config.WithRegion("us-east-2"),
	)
	if err != nil {
		log.Fatalf("load AWS config: %v", err)
	}

	driver, err := s3driver.NewDriver(s3driver.Options{
		Client: awssdkv2.NewClient(s3.NewFromConfig(cfg)),
		Bucket: s3driver.StaticBucket("my-temporal-payloads"),
	})
	if err != nil {
		log.Fatalf("create S3 driver: %v", err)
	}
	// @@@SNIPEND
	return driver
}

// CreateS3MRAPDriver creates an S3 storage driver that uses a Multi-Region Access Point.
func CreateS3MRAPDriver(cfg aws.Config) (converter.StorageDriver, error) {
	// @@@SNIPSTART go-external-storage-s3-mrap
	driver, err := s3driver.NewDriver(s3driver.Options{
		Client: awssdkv2.NewClient(s3.NewFromConfig(cfg)),
		Bucket: s3driver.StaticBucket("arn:aws:s3::123456789012:accesspoint/mfzwi23gnjvgw.mrap"),
	})
	// @@@SNIPEND
	return driver, err
}

// CreateGCSDriver creates a storage driver for a Google Cloud Storage bucket.
func CreateGCSDriver() converter.StorageDriver {
	// @@@SNIPSTART go-external-storage-gcs-driver
	gcsClient, err := storage.NewClient(context.Background())
	if err != nil {
		log.Fatalf("create GCS client: %v", err)
	}

	driver, err := gcsdriver.NewDriver(gcsdriver.Options{
		Client: gcssdk.NewClient(gcsClient),
		Bucket: gcsdriver.StaticBucket("my-temporal-payloads"),
	})
	if err != nil {
		log.Fatalf("create GCS driver: %v", err)
	}
	// @@@SNIPEND
	return driver
}
