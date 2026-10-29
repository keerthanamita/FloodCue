<?php
declare(strict_types=1);
require_once __DIR__ . '/bootstrap.php';
if ($_SERVER['REQUEST_METHOD'] !== 'POST') sendJson(405, ['ok'=>false,'error'=>'method_not_allowed']);
try {
    $admin = authenticateFirebaseUid();
    global $config;
    if ($admin !== (string)($config['qr_admin_uid'] ?? '')) sendJson(403,['ok'=>false,'error'=>'admin_required']);
    $body=readJsonBody(); $uid=trim((string)($body['firebase_uid']??''));
    if ($uid==='' || strlen($uid)>128) sendJson(400,['ok'=>false,'error'=>'invalid_firebase_uid']);
    $pdo=getDatabase();
    $pdo->prepare('INSERT INTO emergency_qr_authorized_users (firebase_uid,active) VALUES (:uid,1) ON DUPLICATE KEY UPDATE active=1')->execute(['uid'=>$uid]);
    sendJson(200,['ok'=>true,'firebase_uid'=>$uid,'active'=>true]);
} catch (Throwable $e) { error_log('FloodCue authorize_emergency_scanner error: '.$e->getMessage()); sendJson(500,['ok'=>false,'error'=>'server_error']); }
