# @@@SNIPSTART dotnet-cloud-run-worker-dockerfile
FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build

WORKDIR /src
COPY *.csproj ./
RUN dotnet restore
COPY . .
RUN dotnet publish -c Release -o /out

FROM mcr.microsoft.com/dotnet/runtime:10.0

WORKDIR /app
COPY --from=build /out ./
CMD ["dotnet", "CloudRunWorker.dll"]
# @@@SNIPEND
