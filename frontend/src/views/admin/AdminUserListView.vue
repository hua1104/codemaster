<template>
    <div class="admin-user-list-view">
        <el-card shadow="never">
            <template #header>
                <div class="card-header">
                    <h2>用户列表</h2>
                    <el-button 
                        type="primary" 
                        :icon="Plus"
                        @click="openCreateDialog"
                    >
                        新增用户
                    </el-button>
                </div>
            </template>

            <div class="filter-bar">
                <el-input 
                    v-model="queryForm.keyword" 
                    placeholder="搜索用户名或邮箱" 
                    clearable 
                    style="width: 300px; margin-right: 15px;"
                    @clear="handleSearch"
                    @keyup.enter="handleSearch"
                />
                <el-select 
                    v-model="queryForm.role" 
                    placeholder="筛选角色" 
                    clearable 
                    style="width: 150px; margin-right: 15px;"
                    @change="handleSearch"
                >
                    <el-option label="管理员" value="ADMIN" />
                    <el-option label="学生" value="STUDENT" />
                </el-select>
                <el-button type="primary" @click="handleSearch">查询</el-button>
            </div>

            <el-table 
                :data="userList" 
                v-loading="loading" 
                class="user-table" 
            >
                <el-table-column type="index" label="序号" width="60" />
                <el-table-column prop="username" label="用户名" min-width="150" />
                <el-table-column prop="email" label="邮箱" min-width="200" />
                <el-table-column prop="role" label="角色" width="100">
                    <template #default="{ row }">
                        <el-tag :type="getTagType(row.role)">
                            {{ row.role === 'ADMIN' ? '管理员' : '学生' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="createdAt" label="注册时间" width="180" />
                
                <el-table-column label="操作" width="150" fixed="right">
                    <template #default="{ row }">
                        <el-button link type="warning" size="small" @click="handleEditRole(row.id)">修改角色</el-button>
                        <el-button link type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <div class="pagination-container">
                <el-pagination
                    @current-change="handlePageChange"
                    :current-page="queryForm.page"
                    :page-size="queryForm.size"
                    :total="total"
                    layout="total, prev, pager, next, jumper"
                    background
                />
            </div>

            <el-dialog
                v-model="createDialogVisible"
                title="新增用户"
                width="500px"
            >
                <el-form label-width="100px" class="user-info-form">
                    <el-form-item label="用户名">
                        <el-input v-model="createForm.username" placeholder="请输入用户名" />
                    </el-form-item>
                    <el-form-item label="邮箱">
                        <el-input v-model="createForm.email" placeholder="请输入邮箱" />
                    </el-form-item>
                    <el-form-item label="密码">
                        <el-input v-model="createForm.password" placeholder="请输入初始密码" type="password" show-password />
                    </el-form-item>
                    <el-form-item label="角色">
                        <el-select v-model="createForm.role" placeholder="请选择角色" style="width: 160px;">
                            <el-option label="管理员" value="ADMIN" />
                            <el-option label="学生" value="STUDENT" />
                        </el-select>
                    </el-form-item>

                    <template v-if="createForm.role === 'STUDENT'">
                        <el-form-item label="学号">
                            <el-input v-model="createForm.studentId" placeholder="请输入学号" />
                        </el-form-item>
                        <el-form-item label="姓名">
                            <el-input v-model="createForm.realName" placeholder="请输入姓名" />
                        </el-form-item>
                        <el-form-item label="班级">
                            <el-input v-model="createForm.className" placeholder="请输入班级" />
                        </el-form-item>
                    </template>
                </el-form>

                <template #footer>
                    <span class="dialog-footer">
                        <el-button @click="createDialogVisible = false">取 消</el-button>
                        <el-button type="primary" :loading="createLoading" @click="handleCreate">
                            确 定
                        </el-button>
                    </span>
                </template>
            </el-dialog>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { Plus } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import apiClient from '@/services/apiClient';
import { endpoints } from '@/services/endpoints';

// --- 类型定义 ---
type UserRole = 'ADMIN' | 'STUDENT';
type TagType = 'info' | 'success' | 'danger' | 'warning' | 'primary';

interface User {
    id: number;
    username: string;
    email: string;
    role: UserRole;
    createdAt: string; // ISO 格式时间
}

interface QueryForm {
    page: number;
    size: number;
    keyword: string;
    role: UserRole | '';
}

interface CreateUserForm {
    username: string;
    email: string;
    password: string;
    role: UserRole;
    studentId?: string;
    realName?: string;
    className?: string;
}

// --- 状态管理 ---
const userList = ref<User[]>([]);
const total = ref(0);
const loading = ref(false);
const createDialogVisible = ref(false);
const createLoading = ref(false);

const queryForm = reactive<QueryForm>({
    page: 1,
    size: 10,
    keyword: '',
    role: '',
});

const createForm = reactive<CreateUserForm>({
    username: '',
    email: '',
    password: '',
    role: 'STUDENT',
    studentId: '',
    realName: '',
    className: '',
});

// --- API 调用：获取用户列表（管理员权限） ---
const fetchUserList = async () => {
    loading.value = true;
    try {
        const response = await apiClient.get(endpoints.admin.users, {
            params: {
                page: queryForm.page - 1, // 后端从 0 开始
                size: queryForm.size,
                keyword: queryForm.keyword,
                role: queryForm.role || undefined,
            },
        });

        const pageData = response.data;
        const items = (pageData.content ?? []) as any[];
        userList.value = items.map((u) => ({
            id: u.id,
            username: u.username,
            email: u.email,
            role: u.role,
            createdAt: u.createTime,
        }));
        total.value = pageData.totalElements ?? 0;
        if (typeof pageData.number === 'number') {
            queryForm.page = pageData.number + 1;
        }
        
    } catch (error) {
        ElMessage.error('加载用户列表失败，请检查 API 接口。');
        console.error('Fetch User List Error:', error);
    } finally {
        loading.value = false;
    }
};

// --- 操作函数 ---
const handleSearch = () => {
    queryForm.page = 1; 
    fetchUserList();
};

const handlePageChange = (newPage: number) => {
    queryForm.page = newPage;
    fetchUserList();
};

const openCreateDialog = () => {
    createForm.username = '';
    createForm.email = '';
    createForm.password = '';
    createForm.role = 'STUDENT';
    createForm.studentId = '';
    createForm.realName = '';
    createForm.className = '';
    createDialogVisible.value = true;
};

const handleCreate = async () => {
    if (!createForm.username.trim()) {
        ElMessage.warning('请输入用户名');
        return;
    }
    if (!createForm.password.trim()) {
        ElMessage.warning('请输入初始密码');
        return;
    }

    createLoading.value = true;
    try {
        await apiClient.post(endpoints.admin.users, {
            username: createForm.username,
            email: createForm.email,
            password: createForm.password,
            role: createForm.role,
            studentId: createForm.role === 'STUDENT' ? createForm.studentId : undefined,
            realName: createForm.realName,
            className: createForm.role === 'STUDENT' ? createForm.className : undefined,
        });

        ElMessage.success('用户创建成功');
        createDialogVisible.value = false;
        fetchUserList();
    } catch (error) {
        ElMessage.error('创建用户失败，请检查输入或稍后重试');
        console.error('Create User Error:', error);
    } finally {
        createLoading.value = false;
    }
};

const handleEditRole = (id: number) => {
    // 实际逻辑应弹出对话框供管理员修改用户角色
    ElMessage.info(`尝试修改用户 ID: ${id} 的角色`);
};

const handleDelete = (id: number) => {
    ElMessageBox.confirm(
        '此操作将永久删除该用户，是否继续?',
        '警告',
        {
            confirmButtonText: '确定删除',
            cancelButtonText: '取消',
            type: 'warning',
        }
    ).then(async () => {
        try {
            await apiClient.delete(`${endpoints.admin.users}/${id}`);
            ElMessage.success('删除成功！');
            fetchUserList();
        } catch (error) {
            ElMessage.error('删除失败，请重试！');
            console.error('Delete User Error:', error);
        }
    }).catch(() => {
        // 取消删除操作
    });
};

// --- 辅助函数 ---
const getTagType = (role: UserRole): TagType => {
    if (role === 'ADMIN') {
        return 'danger';
    }
    return 'success';
};

// --- 生命周期 ---
onMounted(() => {
    fetchUserList();
});
</script>

<style scoped>
.user-table {
    width: 100%;
    margin-top: 20px;
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.card-header h2 {
    font-size: 20px;
    margin: 0;
}

.filter-bar {
    display: flex;
    align-items: center;
    padding-bottom: 10px;
}

.pagination-container {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
}
</style>
