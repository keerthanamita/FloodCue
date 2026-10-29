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
    $s=getDatabase()->prepare('UPDATE emergency_qr_authorized_users SET active=0 WHERE firebase_uid=:uid'); $s->execute(['uid'=>$uid]);
    sendJson(200,['ok'=>true,'firebase_uid'=>$uid,'active'=>false]);
} catch (Throwable $e) { error_log('FloodCue revoke_emergency_scanner error: '.$e->getMessage()); sendJson(500,['ok'=>false,'error'=>'server_error']); }
