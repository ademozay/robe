//@ sourceURL=ProfileManagement.js
define([
    'text!html/ProfileManagement.html',

    'kendo/kendo.button.min',
    'robe/AlertDialog'
], function (view) {
    var ProfileManagementView = Backbone.View.extend({
        render: function () {
            $('#dialogMessage').append(view);
            this.initial();
        },
        data: null,
        initial: function () {
            var me = this;
            $.ajax({
                type: "GET",
                url: AdminApp.getBackendURL() + "user/email/" + $.cookie.read("userEmail"),
                contentType: "application/json",
                success: function (response) {
                    $("#userEmail").val(response.email);
                    $("#userName").val(response.name);
                    $("#userSurname").val(response.surname);
                    me.data = response;
                }
            });

            $("#btnProfileManagement").kendoButton({
                click: this.onBtnProfileManagement
            });
        },

        onBtnProfileManagement: function () {
            data.email = $("#userEmail").val();
            data.name = $("#userName").val();
            data.surname = $("#userSurname").val();

            $.ajax({
                type: "POST",
                url: AdminApp.getBackendURL() + "user",
                data: JSON.stringify(data),
                contentType: "application/json; charset=utf-8",
                success: function (response) {
                    console.log(response);
                    showToast("success", "Profil bilgileriniz başarı ile güncellendi.");

                    /*  LOGOUT  */
                    $.cookie.destroy("auth-token");
                    location.reload();
                },
                error: function (jqXHR, textStatus, errorThrown) {
                    console.log(errorThrown);
                    showToast("error", "Güncelleme esnasında bir hata oluştu.");
                }
            });
        }
    });
    return ProfileManagementView;
});