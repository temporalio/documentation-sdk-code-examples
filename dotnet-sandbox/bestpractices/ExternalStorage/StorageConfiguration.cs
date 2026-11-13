using Temporalio.Converters;

namespace TemporalioDocs.ExternalStorageExample;

public static class StorageConfiguration
{
    public static DataConverter WithThreshold(IStorageDriver driver)
    {
        // @@@SNIPSTART dotnet-external-storage-threshold
        var dataConverter = DataConverter.Default with
        {
            ExternalStorage = new ExternalStorage(driver)
            {
                PayloadSizeThreshold = 256 * 1024,
            },
        };
        // @@@SNIPEND
        return dataConverter;
    }

    public static ExternalStorage ForMigration(IStorageDriver preferred, IStorageDriver legacy)
    {
        // @@@SNIPSTART dotnet-external-storage-multiple-drivers
        var storage = new ExternalStorage(
            new IStorageDriver[] { preferred, legacy },
            (context, payload) => preferred);
        // @@@SNIPEND
        return storage;
    }
}
