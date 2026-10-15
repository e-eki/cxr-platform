import { useState, useEffect, useRef } from 'react';
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Form,
  Input,
  Layout,
  Space,
  Spin,
  Tag,
  Typography,
  message,
} from 'antd';
import { ArrowLeftOutlined, ReloadOutlined } from '@ant-design/icons';
import { useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getCase, getCaseImageUrl, reviewCase } from '../api/cases';
import type { CaseStatus, Prediction, ReviewDecision } from '../types/api';

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

/** SVG-оверлей с bbox-ами поверх изображения. */
function BBoxOverlay({ predictions }: { predictions: Prediction[] }) {
  return (
    <svg
      viewBox="0 0 1 1"
      preserveAspectRatio="none"
      style={{
        position: 'absolute',
        top: 0,
        left: 0,
        width: '100%',
        height: '100%',
        pointerEvents: 'none',
      }}
    >
      {predictions.map((p, idx) => (
        <g key={idx}>
          <rect
            x={p.xMin}
            y={p.yMin}
            width={p.xMax - p.xMin}
            height={p.yMax - p.yMin}
            fill="rgba(255, 0, 0, 0.12)"
            stroke="red"
            strokeWidth={2}
            vectorEffect="non-scaling-stroke"
          />
          <text
            x={p.xMin}
            y={p.yMin}
            dy={-4}
            fill="red"
            style={{ fontSize: 14, fontWeight: 600 }}
          >
            {p.className} {(p.confidence * 100).toFixed(1)}%
          </text>
        </g>
      ))}
    </svg>
  );
}

export default function CaseDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const imgRef = useRef<HTMLImageElement>(null);
  const [imageLoaded, setImageLoaded] = useState(false);
  const [form] = Form.useForm();
  const queryClient = useQueryClient();

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['case', id],
    queryFn: () => getCase(id!),
    enabled: Boolean(id),
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status === 'UPLOADED' || status === 'PROCESSING' ? 3000 : false;
    },
  });

  useEffect(() => {
    setImageLoaded(false);
  }, [id]);

  const reviewMutation = useMutation({
    mutationFn: (values: { decision: ReviewDecision; comment?: string }) =>
      reviewCase(id!, values),
    onSuccess: () => {
      message.success('Ревью сохранено');
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['case', id] });
      queryClient.invalidateQueries({ queryKey: ['cases'] });
    },
    onError: (err: any) => {
      const detail =
        err.response?.data?.message || 'Не удалось сохранить ревью';
      message.error(detail);
    },
  });

  const handleReview = async (values: { comment?: string }) => {
    const decision = form.getFieldValue('decision') as ReviewDecision | undefined;
    if (!decision) {
      message.error('Выберите решение: подтвердить или отклонить');
      return;
    }
    reviewMutation.mutate({ decision, comment: values.comment });
  };

  if (isLoading) {
    return (
      <Layout style={{ minHeight: '100vh' }}>
        <Content
          style={{
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
          }}
        >
          <Spin size="large" />
        </Content>
      </Layout>
    );
  }

  if (!data) {
    return (
      <Layout style={{ minHeight: '100vh' }}>
        <Content className="page-container">
          <Alert type="error" message="Кейс не найден" />
        </Content>
      </Layout>
    );
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
        }}
      >
        <Title level={4} style={{ color: 'white', margin: 0 }}>
          CXR Platform
        </Title>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={() => refetch()}>
            Обновить
          </Button>
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/cases')}>
            Назад
          </Button>
        </Space>
      </Header>

      <Content className="page-container">
        <Card title={`Кейс ${data.id.slice(0, 8)}…`} style={{ marginBottom: 16 }}>
          <Descriptions column={2} size="small">
            <Descriptions.Item label="Пациент">{data.patientId}</Descriptions.Item>
            <Descriptions.Item label="Статус">
              <Tag color={STATUS_COLORS[data.status]}>
                {STATUS_LABELS[data.status]}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Создан">
              {new Date(data.createdAt).toLocaleString('ru-RU')}
            </Descriptions.Item>
            <Descriptions.Item label="Очагов найдено">
              {data.predictions.length}
            </Descriptions.Item>
            {data.comment && (
              <Descriptions.Item label="Комментарий" span={2}>
                {data.comment}
              </Descriptions.Item>
            )}
          </Descriptions>
        </Card>

        <Card title="Снимок" styles={{ body: { padding: 0 } }}>
          {!imageLoaded && (
            <div style={{ padding: 48, textAlign: 'center' }}>
              <Spin tip="Загрузка снимка..." />
            </div>
          )}
          <div
            style={{
              position: 'relative',
              display: 'inline-block',
              maxWidth: '100%',
            }}
          >
            <img
              ref={imgRef}
              src={getCaseImageUrl(data.id)}
              alt="CXR"
              onLoad={() => setImageLoaded(true)}
              style={{
                display: imageLoaded ? 'block' : 'none',
                maxWidth: '100%',
                height: 'auto',
              }}
            />
            {imageLoaded && data.predictions.length > 0 && (
              <BBoxOverlay predictions={data.predictions} />
            )}
          </div>
        </Card>

        {data.status === 'READY_FOR_REVIEW' && (
          <Card title="Ревью" style={{ marginTop: 16 }}>
            <Form form={form} layout="vertical" onFinish={handleReview}>
              <Form.Item label="Комментарий (опционально)" name="comment">
                <Input.TextArea
                  rows={3}
                  placeholder="Замечания по снимку или решению модели"
                  maxLength={1000}
                  showCount
                />
              </Form.Item>
              <Space>
                <Button
                  type="primary"
                  htmlType="submit"
                  onClick={() => form.setFieldValue('decision', 'CONFIRMED')}
                  loading={reviewMutation.isPending}
                >
                  Подтвердить
                </Button>
                <Button
                  danger
                  htmlType="submit"
                  onClick={() => form.setFieldValue('decision', 'REJECTED')}
                  loading={reviewMutation.isPending}
                >
                  Отклонить
                </Button>
              </Space>
            </Form>
          </Card>
        )}

        {data.status === 'CONFIRMED' && (
          <Alert
            type="success"
            showIcon
            message="Снимок подтверждён врачом"
            description={data.comment || 'Без комментария'}
            style={{ marginTop: 16 }}
          />
        )}

        {data.status === 'REJECTED' && (
          <Alert
            type="error"
            showIcon
            message="Снимок отклонён врачом"
            description={data.comment || 'Без комментария'}
            style={{ marginTop: 16 }}
          />
        )}
      </Content>
    </Layout>
  );
}