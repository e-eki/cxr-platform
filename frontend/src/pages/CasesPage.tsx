import { useState } from 'react';
import {
  Button,
  Card,
  Empty,
  Form,
  Input,
  Layout,
  Modal,
  Space,
  Table,
  Tag,
  Typography,
  Upload,
  message,
} from 'antd';
import type { UploadFile } from 'antd';
import { LogoutOutlined, PlusOutlined, UploadOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createCase, listCases } from '../api/cases';
import { logout } from '../api/auth';
import { useAuth } from '../auth/AuthContext';
import type { CaseResponse, CaseStatus } from '../types/api';

const { Header, Content } = Layout;
const { Title } = Typography;

const STATUS_COLORS: Record<CaseStatus, string> = {
  UPLOADED: 'default',
  PROCESSING: 'processing',
  READY_FOR_REVIEW: 'warning',
  CONFIRMED: 'success',
  REJECTED: 'error',
  FAILED: 'error',
};

const STATUS_LABELS: Record<CaseStatus, string> = {
  UPLOADED: 'Загружен',
  PROCESSING: 'Обработка',
  READY_FOR_REVIEW: 'Готов к ревью',
  CONFIRMED: 'Подтверждён',
  REJECTED: 'Отклонён',
  FAILED: 'Ошибка',
};

export default function CasesPage() {
  const navigate = useNavigate();
  const { logout: setLogout } = useAuth();
  const queryClient = useQueryClient();

  const [modalOpen, setModalOpen] = useState(false);
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [form] = Form.useForm();

  const { data, isLoading } = useQuery({
    queryKey: ['cases'],
    queryFn: () => listCases({ page: 0, size: 50 }),
    // Автообновление каждые 5 секунд — чтобы видеть переход статусов
    refetchInterval: 5000,
  });

  const createMutation = useMutation({
    mutationFn: ({ patientId, file }: { patientId: string; file: File }) =>
      createCase(patientId, file),
    onSuccess: () => {
      message.success('Кейс создан, ML-анализ запущен');
      setModalOpen(false);
      setFileList([]);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['cases'] });
    },
    onError: () => message.error('Не удалось создать кейс'),
  });

  const handleSubmit = async () => {
    const values = await form.validateFields();
    const file = fileList[0]?.originFileObj;
    if (!file) {
      message.error('Выберите файл');
      return;
    }
    createMutation.mutate({ patientId: values.patientId, file });
  };

  const handleLogout = () => {
    logout();
    setLogout();
    navigate('/login');
  };

  const columns = [
    {
      title: 'ID кейса',
      dataIndex: 'id',
      key: 'id',
      render: (id: string) => (
        <a onClick={() => navigate(`/cases/${id}`)}>
          {id.slice(0, 8)}…
        </a>
      ),
    },
    { title: 'Пациент', dataIndex: 'patientId', key: 'patientId' },
    {
      title: 'Статус',
      dataIndex: 'status',
      key: 'status',
      render: (status: CaseStatus) => (
        <Tag color={STATUS_COLORS[status]}>{STATUS_LABELS[status]}</Tag>
      ),
    },
    {
      title: 'Очагов найдено',
      dataIndex: 'predictionsCount',
      key: 'predictionsCount',
      render: (count: number) => count,
    },
    {
      title: 'Создан',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (iso: string) => new Date(iso).toLocaleString('ru-RU'),
    },
    {
      title: '',
      key: 'actions',
      render: (_: unknown, record: CaseResponse) => (
        <Button size="small" onClick={() => navigate(`/cases/${record.id}`)}>
          Открыть
        </Button>
      ),
    },
  ];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Title level={4} style={{ color: 'white', margin: 0 }}>
          CXR Platform
        </Title>
        <Button icon={<LogoutOutlined />} onClick={handleLogout}>
          Выйти
        </Button>
      </Header>

      <Content className="page-container">
        <Card
          title="Кейсы"
          extra={
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>
              Загрузить снимок
            </Button>
          }
        >
          <Table
            dataSource={data?.content ?? []}
            columns={columns}
            rowKey="id"
            loading={isLoading}
            pagination={false}
            locale={{ emptyText: <Empty description="Кейсов пока нет" /> }}
          />
        </Card>

        <Modal
          title="Новый кейс"
          open={modalOpen}
          onCancel={() => setModalOpen(false)}
          onOk={handleSubmit}
          confirmLoading={createMutation.isPending}
          okText="Загрузить"
          cancelText="Отмена"
        >
          <Form form={form} layout="vertical">
            <Form.Item
              label="ID пациента"
              name="patientId"
              rules={[{ required: true, message: 'Введите ID пациента' }]}
            >
              <Input placeholder="Например, P-0001" />
            </Form.Item>
            <Form.Item label="Снимок">
              <Upload
                maxCount={1}
                fileList={fileList}
                beforeUpload={() => false}
                onChange={({ fileList }) => setFileList(fileList)}
              >
                <Button icon={<UploadOutlined />}>Выбрать файл</Button>
              </Upload>
            </Form.Item>
          </Form>
        </Modal>
      </Content>
    </Layout>
  );
}