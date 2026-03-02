#!/usr/bin/env bash
set -e

# ===== 服务器配置 =====
MACHINE_A_HOST="42.194.244.5"
MACHINE_A_USER="root"
MACHINE_A_COMPOSE="docker-compose.app.yml"

MACHINE_B_HOST="106.13.14.164"
MACHINE_B_USER="root"
MACHINE_B_COMPOSE="docker-compose.yml"

PROJECT_DIR="/opt/dango-ai-code-mother"
PLATFORM="linux/amd64"

# ===== 服务与机器映射 =====
MACHINE_A_SERVICES="app-service deploy-nginx"
MACHINE_B_SERVICES="user-service screenshot-app supabase-service supabase-manager"
ALL_SERVICES="$MACHINE_A_SERVICES $MACHINE_B_SERVICES"

# ===== Java 服务统一走: 本地打 jar + 远端 runtime Dockerfile 构建 =====
# 返回: jar路径|runtime-dockerfile|镜像名|maven-pl参数|依赖基础镜像(可空)
get_java_remote_build_info() {
    local svc=$1
    case $svc in
        app-service)
            echo "backend/app/app-service/target/app-service-1.0-SNAPSHOT.jar|backend/Dockerfile.app-runtime|dango-ai-code-app:latest|app/app-service|dango-app-base:1.0" ;;
        user-service)
            echo "backend/user/user-service/target/user-service-1.0-SNAPSHOT.jar|backend/Dockerfile.user-runtime|dango-ai-code-user:latest|user/user-service|" ;;
        screenshot-app)
            echo "backend/screenshot/screenshot-app/target/screenshot-app-1.0-SNAPSHOT.jar|backend/Dockerfile.screenshot-runtime|dango-ai-code-screenshot:latest|screenshot/screenshot-app|dango-screenshot-base:1.0" ;;
        supabase-service)
            echo "backend/supabase/supabase-service/target/supabase-service-1.0-SNAPSHOT.jar|backend/Dockerfile.supabase-runtime|dango-ai-code-supabase:latest|supabase/supabase-service|" ;;
        *)
            echo "" ;;
    esac
}

# ===== 非 Java 服务本地构建（目前仅 supabase-manager） =====
# 返回: 镜像名|Dockerfile|构建上下文|构建参数
get_local_build_info() {
    local svc=$1
    case $svc in
        supabase-manager)
            echo "dango-ai-code-supabase-manager:latest|supabase-manager/Dockerfile|supabase-manager/|" ;;
        deploy-nginx)
            echo "" ;;
        *)
            echo "" ;;
    esac
}

contains_word() {
    local haystack=$1
    local needle=$2
    case " $haystack " in
        *" $needle "*) return 0 ;;
        *) return 1 ;;
    esac
}

# ===== 解析参数 =====
MODE="build"
SERVICES_ARGS=""

for arg in "$@"; do
    if [ "$arg" = "--restart" ] || [ "$arg" = "-r" ]; then
        MODE="restart"
    else
        SERVICES_ARGS="$SERVICES_ARGS $arg"
    fi
done

if [ -z "$(echo "$SERVICES_ARGS" | tr -d ' ')" ]; then
    TARGETS="$ALL_SERVICES"
    echo "部署所有后端服务... (模式: $MODE)"
else
    TARGETS="$SERVICES_ARGS"
    echo "部署指定服务:$TARGETS (模式: $MODE)"
fi

# 判断目标服务属于哪台机器，同时记录机器执行顺序（按输入顺序）
A_TARGETS=""
B_TARGETS=""
A_IMAGES=""
B_IMAGES=""
MACHINE_ORDER=""

for svc in $TARGETS; do
    if contains_word "$MACHINE_A_SERVICES" "$svc"; then
        A_TARGETS="$A_TARGETS $svc"
        contains_word "$MACHINE_ORDER" "A" || MACHINE_ORDER="$MACHINE_ORDER A"
    elif contains_word "$MACHINE_B_SERVICES" "$svc"; then
        B_TARGETS="$B_TARGETS $svc"
        contains_word "$MACHINE_ORDER" "B" || MACHINE_ORDER="$MACHINE_ORDER B"
    else
        echo "错误: 未知服务 '$svc'"
        echo "可用服务: $ALL_SERVICES"
        exit 1
    fi
done

# ===== 构建输入 =====
REMOTE_BUILD_LIST=""
MAVEN_MODULES=""

# ===== 本地构建 =====
if [ "$MODE" = "build" ]; then
    echo ""
    echo "===== 本地构建 ====="

    for svc in $TARGETS; do
        remote_info=$(get_java_remote_build_info "$svc")
        if [ -n "$remote_info" ]; then
            maven_pl=$(echo "$remote_info" | cut -d'|' -f4)
            REMOTE_BUILD_LIST="$REMOTE_BUILD_LIST $svc"
            contains_word "$MAVEN_MODULES" "$maven_pl" || MAVEN_MODULES="$MAVEN_MODULES $maven_pl"
            continue
        fi

        info=$(get_local_build_info "$svc")
        [ -z "$info" ] && continue

        image=$(echo "$info" | cut -d'|' -f1)
        dockerfile=$(echo "$info" | cut -d'|' -f2)
        context=$(echo "$info" | cut -d'|' -f3)
        build_args=$(echo "$info" | cut -d'|' -f4)

        echo ""
        echo "--- 本地构建 $svc → $image ---"
        if [ -n "$build_args" ]; then
            # shellcheck disable=SC2206
            extra_args=($build_args)
            docker buildx build --platform "$PLATFORM" -f "$dockerfile" -t "$image" "${extra_args[@]}" --load "$context"
        else
            docker buildx build --platform "$PLATFORM" -f "$dockerfile" -t "$image" --load "$context"
        fi

        if contains_word "$MACHINE_A_SERVICES" "$svc"; then
            A_IMAGES="$A_IMAGES $image"
        else
            B_IMAGES="$B_IMAGES $image"
        fi
    done

    if [ -n "$(echo "$MAVEN_MODULES" | tr -d ' ')" ]; then
        modules_csv=$(echo "$MAVEN_MODULES" | xargs | tr ' ' ',')
        echo ""
        echo "--- 统一编译 Java 模块 (仅 jar): $modules_csv ---"
        (cd backend && mvn clean package -pl "$modules_csv" -am -Dmaven.test.skip=true -B)
    fi
fi

require_remote_base_image() {
    local host=$1 user=$2 label=$3 svc=$4 image=$5
    [ -z "$image" ] && return 0

    local remote="${user}@${host}"

    ssh -o StrictHostKeyChecking=no "$remote" \
        "if ! docker image inspect ${image} >/dev/null 2>&1; then \
            echo '[错误] ${label} 缺少基础镜像 ${image}（服务: ${svc}）'; \
            echo '请先执行基础镜像初始化，再重新发布'; \
            exit 1; \
        fi"
}

remote_build() {
    local host=$1 user=$2 label=$3 svc=$4

    contains_word "$REMOTE_BUILD_LIST" "$svc" || return 0

    local remote_info
    remote_info=$(get_java_remote_build_info "$svc")
    [ -z "$remote_info" ] && return 0

    local jar_path runtime_dockerfile image base_image
    jar_path=$(echo "$remote_info" | cut -d'|' -f1)
    runtime_dockerfile=$(echo "$remote_info" | cut -d'|' -f2)
    image=$(echo "$remote_info" | cut -d'|' -f3)
    base_image=$(echo "$remote_info" | cut -d'|' -f5)

    if [ ! -f "$jar_path" ]; then
        echo "错误: 未找到 jar 文件 $jar_path"
        exit 1
    fi

    require_remote_base_image "$host" "$user" "$label" "$svc" "$base_image"

    local remote="${user}@${host}"
    local remote_build_dir="${PROJECT_DIR}/build-tmp-${svc}"
    local jar_size
    jar_size=$(du -h "$jar_path" | cut -f1)
    echo ""
    echo "===== 远程构建 $svc → $image (${label}) ====="
    echo "传输 jar ($jar_size) ..."

    ssh -o StrictHostKeyChecking=no "$remote" "mkdir -p ${remote_build_dir}"
    scp -o StrictHostKeyChecking=no "$jar_path" "${remote}:${remote_build_dir}/app.jar"
    scp -o StrictHostKeyChecking=no "$runtime_dockerfile" "${remote}:${remote_build_dir}/Dockerfile"

    echo "远程 docker build ..."
    ssh -o StrictHostKeyChecking=no "$remote" \
        "cd ${remote_build_dir} && DOCKER_BUILDKIT=0 docker build -t ${image} . && rm -rf ${remote_build_dir}"
    echo "$svc 远程构建完成"
}

push_images() {
    local host=$1 user=$2 label=$3
    shift 3
    local images="$*"

    [ -z "$(echo "$images" | tr -d ' ')" ] && return

    echo ""
    echo "===== 推送镜像到 ${label} (${host}) ====="
    echo "镜像: $images"
    local size
    size=$(docker image inspect $images --format='{{.Size}}' 2>/dev/null | awk '{s+=$1} END {printf "%.0f", s/1024/1024}')
    echo "预估大小: ~${size}MB (压缩后更小)"

    if command -v pv &>/dev/null; then
        docker save $images | gzip | pv -s "${size}m" | ssh -o StrictHostKeyChecking=no "${user}@${host}" "gunzip | docker load"
    else
        docker save $images | gzip | ssh -o StrictHostKeyChecking=no "${user}@${host}" "gunzip | docker load"
    fi
    echo "${label} 镜像加载完成"
}

# ===== 同步配置文件并启动服务 =====
deploy_services() {
    local host=$1 user=$2 compose=$3 label=$4
    shift 4
    local services="$*"

    local remote="${user}@${host}"

    echo ""
    echo "===== ${label} - 同步配置文件 ====="
    scp -o StrictHostKeyChecking=no "$compose" "${remote}:${PROJECT_DIR}/${compose}"

    # Machine A 额外同步 nginx 配置
    if [ "$compose" = "$MACHINE_A_COMPOSE" ]; then
        scp -o StrictHostKeyChecking=no deploy-nginx.conf "${remote}:${PROJECT_DIR}/deploy-nginx.conf"
    fi

    echo ""
    echo "===== ${label} - 启动: ${services} ====="
    ssh -o StrictHostKeyChecking=no "$remote" \
        "cd ${PROJECT_DIR} && docker compose -f ${compose} up -d ${services}"
}

# ===== 执行部署（按输入服务对应机器顺序） =====
for machine in $MACHINE_ORDER; do
    case $machine in
        A)
            [ "$MODE" = "build" ] && push_images "$MACHINE_A_HOST" "$MACHINE_A_USER" "机器 A" $A_IMAGES
            if [ "$MODE" = "build" ]; then
                for svc in $A_TARGETS; do
                    remote_build "$MACHINE_A_HOST" "$MACHINE_A_USER" "机器 A" "$svc"
                done
            fi
            deploy_services "$MACHINE_A_HOST" "$MACHINE_A_USER" "$MACHINE_A_COMPOSE" "机器 A" $A_TARGETS
            ;;
        B)
            [ "$MODE" = "build" ] && push_images "$MACHINE_B_HOST" "$MACHINE_B_USER" "机器 B" $B_IMAGES
            if [ "$MODE" = "build" ]; then
                for svc in $B_TARGETS; do
                    remote_build "$MACHINE_B_HOST" "$MACHINE_B_USER" "机器 B" "$svc"
                done
            fi
            deploy_services "$MACHINE_B_HOST" "$MACHINE_B_USER" "$MACHINE_B_COMPOSE" "机器 B" $B_TARGETS
            ;;
    esac
done

echo ""
echo "部署完成"

# ===== 清理本地 amd64 镜像（Mac 上用不了） =====
if [ "$MODE" = "build" ]; then
    ALL_IMAGES="$A_IMAGES $B_IMAGES"
    if [ -n "$(echo "$ALL_IMAGES" | tr -d ' ')" ]; then
        echo ""
        echo "===== 清理本地 amd64 镜像 ====="
        docker rmi $ALL_IMAGES 2>/dev/null && echo "已清理" || echo "清理跳过"
    fi
fi
