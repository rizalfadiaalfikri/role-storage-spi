# # Stage 1: Build Maven project
# FROM maven:3.9-eclipse-temurin-17 AS builder

# WORKDIR /build

# # Copy pom.xml and source code
# COPY pom.xml .
# COPY src ./src

# # Build the project
# RUN mvn clean package -DskipTests

# # Stage 2: Keycloak dengan custom provider
# FROM quay.io/keycloak/keycloak:26.0.0

# # Copy custom provider JAR ke Keycloak providers directory
# COPY --from=builder /build/target/role-storage-spi-*.jar /opt/keycloak/providers/

# # Build Keycloak dengan optimasi
# RUN /opt/keycloak/bin/kc.sh build --db=postgres --health-enabled=true --metrics-enabled=true --transaction-xa-enabled=true --features=preview,token-exchange,admin-fine-grained-authz:v1

# ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]

# CMD ["start", "--optimized"]

# Stage 1: Build Maven project
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Keycloak
FROM quay.io/keycloak/keycloak:26.0.0

ARG JAVA_OPTS_APPEND
ENV JAVA_OPTS_APPEND=$JAVA_OPTS_APPEND

COPY --from=0 /build/target/role-storage-spi-*.jar /opt/keycloak/providers/

# Build Keycloak dengan optimasi
# Note: role-storage bukan feature yang perlu diaktifkan - Role Storage Provider adalah SPI
# yang akan otomatis terdeteksi jika JAR sudah di-copy ke /opt/keycloak/providers/
RUN /opt/keycloak/bin/kc.sh build \
    --db=postgres \
    --health-enabled=true \
    --metrics-enabled=true \
    --transaction-xa-enabled=true \
    --features=preview,token-exchange,admin-fine-grained-authz:v1

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--optimized"]