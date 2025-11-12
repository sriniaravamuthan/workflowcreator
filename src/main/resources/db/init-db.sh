#!/bin/bash

# ============================================================================
# HMIS Workflow Engine - Database Initialization Script
# ============================================================================
# This script initializes the database with schema and optional sample data
# Supports H2, PostgreSQL, and MySQL
# ============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default values
DB_TYPE="postgresql"
DB_HOST="localhost"
DB_PORT="5432"
DB_NAME="workflow_db"
DB_USER="postgres"
LOAD_SAMPLE_DATA="false"

# ============================================================================
# FUNCTIONS
# ============================================================================

print_header() {
    echo -e "\n${GREEN}========================================${NC}"
    echo -e "${GREEN}$1${NC}"
    echo -e "${GREEN}========================================${NC}\n"
}

print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

show_usage() {
    cat << EOF
Usage: $0 [OPTIONS]

OPTIONS:
    -t, --type TYPE           Database type: postgresql, mysql, h2 (default: postgresql)
    -h, --host HOST           Database host (default: localhost)
    -p, --port PORT           Database port (default: 5432 for PostgreSQL, 3306 for MySQL)
    -d, --database NAME       Database name (default: workflow_db)
    -u, --user USER           Database user (default: postgres)
    -s, --sample-data         Load sample data (default: false)
    --help                    Show this help message

EXAMPLES:
    # Initialize PostgreSQL database
    $0 --type postgresql --user postgres --database workflow_db --sample-data

    # Initialize MySQL database
    $0 --type mysql --user root --port 3306 --sample-data

    # Initialize H2 (in-memory - development only)
    $0 --type h2

EOF
    exit 0
}

# ============================================================================
# ARGUMENT PARSING
# ============================================================================

while [[ $# -gt 0 ]]; do
    case $1 in
        -t|--type)
            DB_TYPE="$2"
            shift 2
            ;;
        -h|--host)
            DB_HOST="$2"
            shift 2
            ;;
        -p|--port)
            DB_PORT="$2"
            shift 2
            ;;
        -d|--database)
            DB_NAME="$2"
            shift 2
            ;;
        -u|--user)
            DB_USER="$2"
            shift 2
            ;;
        -s|--sample-data)
            LOAD_SAMPLE_DATA="true"
            shift
            ;;
        --help)
            show_usage
            ;;
        *)
            print_error "Unknown option: $1"
            show_usage
            ;;
    esac
done

# ============================================================================
# MAIN LOGIC
# ============================================================================

print_header "HMIS Workflow Engine - Database Initialization"

print_info "Database Type: $DB_TYPE"
print_info "Database Host: $DB_HOST"
print_info "Database Port: $DB_PORT"
print_info "Database Name: $DB_NAME"
print_info "Database User: $DB_USER"
print_info "Load Sample Data: $LOAD_SAMPLE_DATA"

# ============================================================================
# PostgreSQL
# ============================================================================

if [ "$DB_TYPE" = "postgresql" ]; then
    print_header "Setting up PostgreSQL"

    # Set default port for PostgreSQL
    if [ "$DB_PORT" = "5432" ]; then
        DB_PORT="5432"
    fi

    print_info "Creating database: $DB_NAME"

    # Create database (suppress already exists error)
    createdb -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" "$DB_NAME" 2>/dev/null || \
        print_warning "Database $DB_NAME may already exist"

    print_info "Creating schema..."
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$(dirname "$0")/ddl/01-schema.sql"

    if [ "$LOAD_SAMPLE_DATA" = "true" ]; then
        print_info "Loading sample data..."
        psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$(dirname "$0")/data/sample-data.sql"
        print_info "Sample data loaded successfully"
    fi

    print_info "PostgreSQL initialization completed successfully!"

# ============================================================================
# MySQL
# ============================================================================

elif [ "$DB_TYPE" = "mysql" ]; then
    print_header "Setting up MySQL"

    # Set default port for MySQL
    if [ "$DB_PORT" = "5432" ]; then
        DB_PORT="3306"
    fi

    print_info "Creating database: $DB_NAME"

    # Create database (suppress error if already exists)
    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p -e "CREATE DATABASE IF NOT EXISTS $DB_NAME;" 2>/dev/null || \
        print_warning "Database creation encountered an issue"

    print_info "Creating schema..."
    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p "$DB_NAME" < "$(dirname "$0")/ddl/01-schema.sql"

    if [ "$LOAD_SAMPLE_DATA" = "true" ]; then
        print_info "Loading sample data..."
        mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p "$DB_NAME" < "$(dirname "$0")/data/sample-data.sql"
        print_info "Sample data loaded successfully"
    fi

    print_info "MySQL initialization completed successfully!"

# ============================================================================
# H2 (In-Memory)
# ============================================================================

elif [ "$DB_TYPE" = "h2" ]; then
    print_header "Setting up H2 (In-Memory Database)"

    print_info "H2 database is in-memory and automatically initialized by Hibernate"
    print_info "Schema will be created on application startup"
    print_warning "H2 is recommended for development only"
    print_info "For production, use PostgreSQL or MySQL"

# ============================================================================
# Invalid Database Type
# ============================================================================

else
    print_error "Invalid database type: $DB_TYPE"
    print_info "Supported types: postgresql, mysql, h2"
    exit 1
fi

print_header "Database Initialization Complete!"
print_info "Next steps:"
print_info "1. Configure database connection in application.yml"
print_info "2. Set hibernate.ddl-auto to 'validate' for production"
print_info "3. Run the application with: mvn spring-boot:run"
print_info "4. Access API at: http://localhost:8080/swagger-ui.html"

exit 0
