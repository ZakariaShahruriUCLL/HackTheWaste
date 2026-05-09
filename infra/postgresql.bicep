// =============================================================================
// Leuven Go — Azure Database for PostgreSQL Flexible Server
// =============================================================================
// Stores the photo-feed records: who posted, which clan, AI cleanliness score,
// and the Azure blob URL of the actual photo. The Spring Boot app reads/writes
// via JDBC; the React feed page queries the Spring API.
//
// Connection string format for application-prod.yml:
//   jdbc:postgresql://{serverFqdn}:5432/{databaseName}?sslmode=require
// =============================================================================

@description('Azure region — must be one of your subscription allowed regions.')
@allowed(['polandcentral', 'francecentral', 'swedencentral', 'germanywestcentral', 'switzerlandnorth'])
param location string = 'francecentral'

@description('PostgreSQL server name — globally unique, 3–63 chars, lowercase letters, digits and hyphens.')
@minLength(3)
@maxLength(63)
param serverName string

@description('Name of the application database.')
param databaseName string = 'leuvengo'

@description('Administrator login name.')
param adminLogin string = 'leuvenadmin'

@description('Administrator password. Must meet Azure complexity requirements.')
@secure()
param adminPassword string

@description('Environment tag.')
@allowed(['dev', 'staging', 'prod'])
param environment string = 'dev'

// ---------------------------------------------------------------------------
// PostgreSQL Flexible Server — Burstable B1ms (cheapest tier, fine for dev)
// Upgrade to GeneralPurpose D2s_v3 or higher for production load.
// ---------------------------------------------------------------------------

resource postgresServer 'Microsoft.DBforPostgreSQL/flexibleServers@2022-12-01' = {
  name: serverName
  location: location
  sku: {
    name: 'Standard_B1ms'
    tier: 'Burstable'
  }
  tags: {
    project: 'leuven-go'
    environment: environment
    team: 'ReCode'
  }
  properties: {
    version: '16'
    administratorLogin: adminLogin
    administratorLoginPassword: adminPassword
    authConfig: {
      activeDirectoryAuth: 'Disabled'
      passwordAuth: 'Enabled'
    }
    storage: {
      storageSizeGB: 32 // minimum; auto-grow can be enabled via portal
    }
    backup: {
      backupRetentionDays: 7
      geoRedundantBackup: 'Disabled' // enable for production
    }
    highAvailability: {
      mode: 'Disabled' // enable ZoneRedundant for production
    }
  }
}

// Allow connections from other Azure services (e.g. App Service, Azure Functions)
resource firewallAzureServices 'Microsoft.DBforPostgreSQL/flexibleServers/firewallRules@2022-12-01' = {
  parent: postgresServer
  name: 'AllowAzureServices'
  properties: {
    startIpAddress: '0.0.0.0'
    endIpAddress: '0.0.0.0'
  }
}

// Allow all IPs for dev convenience — tighten to your office/VPN IP in production
resource firewallAllowDev 'Microsoft.DBforPostgreSQL/flexibleServers/firewallRules@2022-12-01' = {
  parent: postgresServer
  name: 'AllowAll-DevOnly'
  properties: {
    startIpAddress: '0.0.0.0'
    endIpAddress: '255.255.255.255'
  }
}

// ---------------------------------------------------------------------------
// Application database
// ---------------------------------------------------------------------------

resource database 'Microsoft.DBforPostgreSQL/flexibleServers/databases@2022-12-01' = {
  parent: postgresServer
  name: databaseName
  properties: {
    charset: 'UTF8'
    collation: 'en_US.utf8'
  }
}

// ---------------------------------------------------------------------------
// Outputs — paste these into your application-prod.yml or Azure App Settings
// ---------------------------------------------------------------------------

@description('Fully-qualified server hostname.')
output serverFqdn string = postgresServer.properties.fullyQualifiedDomainName

@description('JDBC connection URL — set as SPRING_DATASOURCE_URL in your app.')
output jdbcUrl string = 'jdbc:postgresql://${postgresServer.properties.fullyQualifiedDomainName}:5432/${databaseName}?sslmode=require'

@description('Database name.')
output databaseName string = databaseName
