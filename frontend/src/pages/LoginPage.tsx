import { useState } from 'react';
import { Button, Card, Form, Input, Tabs, Typography, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import { login, register } from '../api/auth';
import { useAuth } from '../auth/AuthContext';
import type { LoginRequest, RegisterRequest } from '../types/api';

const { Title } = Typography;

export default function LoginPage() {
  const navigate = useNavigate();
  const { login: setAuth } = useAuth();
  const [loading, setLoading] = useState(false);

  const handleLogin = async (values: LoginRequest) => {
    setLoading(true);
    try {
      const response = await login(values);
      setAuth(response.token);
      message.success('Вход выполнен');
      navigate('/cases');
    } catch {
      message.error('Неверный логин или пароль');
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (values: RegisterRequest) => {
    setLoading(true);
    try {
      const response = await register(values);
      setAuth(response.token);
      message.success('Регистрация успешна');
      navigate('/cases');
    } catch {
      message.error('Ошибка регистрации (возможно, логин или email заняты)');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: '#f0f2f5',
      }}
    >
      <Card style={{ width: 420 }}>
        <Title level={3} style={{ textAlign: 'center', marginBottom: 24 }}>
          CXR Platform
        </Title>
        <Tabs
          items={[
            {
              key: 'login',
              label: 'Вход',
              children: (
                <Form layout="vertical" onFinish={handleLogin} autoComplete="off">
                  <Form.Item
                    label="Логин"
                    name="username"
                    rules={[{ required: true, message: 'Введите логин' }]}
                  >
                    <Input size="large" />
                  </Form.Item>
                  <Form.Item
                    label="Пароль"
                    name="password"
                    rules={[{ required: true, message: 'Введите пароль' }]}
                  >
                    <Input.Password size="large" />
                  </Form.Item>
                  <Button
                    type="primary"
                    htmlType="submit"
                    block
                    size="large"
                    loading={loading}
                  >
                    Войти
                  </Button>
                </Form>
              ),
            },
            {
              key: 'register',
              label: 'Регистрация',
              children: (
                <Form layout="vertical" onFinish={handleRegister} autoComplete="off">
                  <Form.Item
                    label="Логин"
                    name="username"
                    rules={[{ required: true, min: 3, message: 'Минимум 3 символа' }]}
                  >
                    <Input size="large" />
                  </Form.Item>
                  <Form.Item
                    label="Email"
                    name="email"
                    rules={[
                      { required: true, message: 'Введите email' },
                      { type: 'email', message: 'Некорректный email' },
                    ]}
                  >
                    <Input size="large" />
                  </Form.Item>
                  <Form.Item
                    label="Пароль"
                    name="password"
                    rules={[{ required: true, min: 6, message: 'Минимум 6 символов' }]}
                  >
                    <Input.Password size="large" />
                  </Form.Item>
                  <Button
                    type="primary"
                    htmlType="submit"
                    block
                    size="large"
                    loading={loading}
                  >
                    Зарегистрироваться
                  </Button>
                </Form>
              ),
            },
          ]}
        />
      </Card>
    </div>
  );
}