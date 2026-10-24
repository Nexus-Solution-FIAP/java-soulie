# Build da API Soulie para o Render.
# A plataforma não tem runtime nativo de Java: o Web Service precisa ser Docker.
#
# Segredos NÃO entram na imagem. DB_*, SECRET_JWT, S3_*, RABBITMQ_* e o resto
# vêm das variáveis de ambiente do serviço em runtime.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./
# Checkout no Windows pode deixar CRLF; o shebang com \r quebra o build no Linux.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

COPY src src
# Testes ficam no CI / ./mvnw verify. O build da imagem só precisa do jar.
RUN ./mvnw -q -B -DskipTests package \
    && cp target/soulie-0.0.1-SNAPSHOT.jar /tmp/app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --uid 1001 --user-group --no-create-home soulie \
    && mkdir -p /app/data/soulie-images \
    && chown -R soulie:soulie /app
COPY --from=build --chown=soulie:soulie /tmp/app.jar /app/app.jar

# A imagem é de produção. O painel do Render ainda pode sobrescrever.
ENV SPRING_PROFILES_ACTIVE=prod
USER soulie

# Render escuta PORT (padrão 10000). application.yaml também lê ${PORT:8080};
# o -Dserver.port cobre o caso de alguém sobrescrever o YAML.
EXPOSE 10000
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-10000} -XX:MaxRAMPercentage=75.0 -jar /app/app.jar"]
